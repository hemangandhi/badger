package edu.cmu.sv.badger.app;

import java.io.File;
import java.util.ArrayList;
import java.util.List;
import java.util.Map;
import java.util.Map.Entry;
import java.util.concurrent.ArrayBlockingQueue;
import java.util.concurrent.BlockingQueue;
import java.util.concurrent.atomic.AtomicInteger;

import edu.cmu.sv.badger.analysis.StateBuilder;
import edu.cmu.sv.badger.listener.ConcreteInput2TrieListener;
import edu.cmu.sv.badger.listener.MetricListener;
import edu.cmu.sv.badger.listener.SymCreteCostListener;
import edu.cmu.sv.badger.listener.TrieGuidanceListener;
import edu.cmu.sv.badger.trie.Trie;
import edu.cmu.sv.badger.trie.Trie.CostStrategy;
import edu.cmu.sv.badger.trie.TrieNode;
import edu.cmu.sv.badger.util.Statistics;
import gov.nasa.jpf.Config;
import gov.nasa.jpf.JPF;
import gov.nasa.jpf.JPF.ExitException;
import gov.nasa.jpf.JPFConfigException;
import gov.nasa.jpf.JPFException;
import gov.nasa.jpf.symbc.Observations;
import gov.nasa.jpf.symbc.SymbolicListener;
import gov.nasa.jpf.symbc.numeric.PathCondition;
import gov.nasa.jpf.util.Pair;

/**
 * Implements the SymExe compartment of Badger.
 * 
 * @author Yannic Noller <nolleryc@gmail.com> - YN
 */
public class SymExe {

    private BadgerInput input;
    private Trie trie;
    private BlockingQueue<Pair<PathCondition, Map<String, Object>>> pcAndSolutionQueue;
    public static AtomicInteger lastId = new AtomicInteger(-1);
    private static AtomicInteger lastTempFileId = new AtomicInteger(-1);
    List<String> alreadyReadInputFiles = new ArrayList<>();

    public static enum ConcreteSPFMode {
        IMPORT, EXPORT;
    }

    public SymExe(BadgerInput input) {
        this.input = input;
        this.trie = new Trie(input.explorationHeuristic);
        lastId.set(input.initialId);
        this.pcAndSolutionQueue = new ArrayBlockingQueue<>(1000);
    }

    public void run() {
        if (input.secUntilFirstCycle > 0) {
            try {
                Thread.sleep(input.secUntilFirstCycle * 1000);
            } catch (InterruptedException e) {
                return;
            }
        }

        boolean firstStep = true;
        while (true) {

            // Read input.
            List<String> newInputfiles;
            if (firstStep) {
                firstStep = false;
                newInputfiles = analyzeInputFiles(input.intialInputDir);
            } else {
                newInputfiles = analyzeInputFiles(
                        input.syncInputdir.isPresent() ? input.syncInputdir.get() : input.intialInputDir);
            }

            // Run one step.
            boolean needsABreak = runStep(newInputfiles);

            // Only make a break if there was at least one file exported.
            if (needsABreak) {
                try {
                    Thread.sleep(input.cycleWaitingSec * 1000);
                } catch (InterruptedException e) {
                    e.printStackTrace();
                    break;
                }
            }

        }
    }

    /**
     * @return Returns True if we need a small break.
     */
    private boolean runStep(List<String> newInputFiles) {

        if (newInputFiles.isEmpty() && trie.isPriorityQueueEmpty()) {
            // If there is no new input (from fuzzer), and we assume that our last run was complete, then
            // here is no need to further analyze or process the trie, because there is no path left to find.
            System.out.println("[SPF] nothing to process, wait for " + input.cycleWaitingSec + " sec ...");
            return true; // there is currently no sense in running further, waiting for fuzzer makes more sense.
        }

        Map<String, String> processedNewInputs = input.ioUtils.processInput(newInputFiles);

        // Extend trie for new input.
        if (!processedNewInputs.isEmpty()) {
            buildTrieFromProcessedInput(processedNewInputs, ConcreteSPFMode.IMPORT);
            Statistics.appendTrieStatistics(input, trie.getStatistics(), pcAndSolutionQueue.size(),
                    alreadyReadInputFiles.size());
        }

        /*
         * Explore new nodes according to the settings. The reason for making a loop here is that we only can select one
         * execution path as guidance, because the choice generator does not support a multi-selection.
         */
        for (int i = 0; i < input.maximumNumberOfSymExeIterations; i++) {

            // Analyze trie.
            TrieNode identifiedNode = input.trieAnalysisMethod.analyze(trie);

            if (input.printTrieAsDot) {
                Trie.storeTrieAsDot(trie, "trie-analyzed.dot");
            }

            // Break the loop if no new node was identified.
            if (identifiedNode == null) {
                break;
            }

            // Replay trie for enabled nodes and extract path conditions for new explored nodes.
            runJPF(trie, input.numberOfAdditionalDecisions, identifiedNode.getInputSize());

            Statistics.appendTrieStatistics(input, trie.getStatistics(), pcAndSolutionQueue.size(),
                    alreadyReadInputFiles.size());

            if (input.printTrieAsDot) {
                Trie.storeTrieAsDot(trie, "trie-explored.dot");
            }

            // Generate input.
            List<String> generatedTmpFiles = generateTmpInputFiles();

            // Read new input files, updated trie, and extract relevant inputs for fuzzer.
            Map<String, String> processedGeneratedTmpFiles = input.ioUtils.processInput(generatedTmpFiles);
            if (!processedGeneratedTmpFiles.isEmpty()) {
                buildTrieFromProcessedInput(processedGeneratedTmpFiles, ConcreteSPFMode.EXPORT);
            }

            if (input.printTrieAsDot) {
                Trie.storeTrieAsDot(trie, "trie-extended.dot");
            }

            Statistics.appendTrieStatistics(input, trie.getStatistics(), pcAndSolutionQueue.size(),
                    alreadyReadInputFiles.size());
        }

        return false;
    }

    private List<String> generateTmpInputFiles() {
        List<String> generatedTmpFiles = new ArrayList<>();
        while (!pcAndSolutionQueue.isEmpty()) {
            try {

                Pair<PathCondition, Map<String, Object>> pcAndSolution = pcAndSolutionQueue.take();

                String outputfile = generateTmpInputfile(pcAndSolution);
                generatedTmpFiles.add(outputfile);

            } catch (InterruptedException e) {
                e.printStackTrace();
                break;
            }
        }
        return generatedTmpFiles;
    }

    private String generateTmpInputfile(Pair<PathCondition, Map<String, Object>> pcAndSolution) {
        String outputfile = input.tmpDir + "/" + String.valueOf(lastTempFileId.incrementAndGet());
        input.ioUtils.generateInputFiles(pcAndSolution._1, pcAndSolution._2, outputfile);

        Statistics.appendGenerationStatistics(input, outputfile);
        Statistics.appendPCMapping(input, outputfile, pcAndSolution._1.toString());

        return (outputfile);
    }

    /**
     * Return files in inputDir that were not already read into trie.
     * 
     * @param inputDir
     * @return list of file names that need to be read.
     */
    private List<String> analyzeInputFiles(String inputDir) {
        File aflQueueFolder = new File(inputDir);

        List<String> newInputFiles = new ArrayList<>();

        for (File inputFile : aflQueueFolder.listFiles()) {
            if (!inputFile.isHidden()) {
                String fileName = inputFile.getAbsolutePath();
                if (alreadyReadInputFiles.contains(fileName)) {
                    continue;
                }
                alreadyReadInputFiles.add(fileName);
                newInputFiles.add(fileName);
            }
        }

        return newInputFiles;
    }

    private Pair<Double, Boolean> runJPF(String targetArgument, String originalFileName, Trie trie,
            ConcreteSPFMode spfMode) {

        if (targetArgument == null) {
            return null;
        }

        System.out.println("Run JPF with argument: " + targetArgument);

        try {
            Config conf = initSPFConfig();
            conf.setProperty("symbolic.collect_constraints", "true");
            conf.setProperty("symbolic.dp", "no_solver"); // symcrete execution, no solver
            conf.setProperty("target.args", input.jpf_argument.replace("@@", targetArgument));

            JPF jpf = new JPF(conf);

            SymbolicListener symbolicListener = new SymbolicListener(conf, jpf);
            jpf.addListener(symbolicListener);

            StateBuilder stateBuilder = null;
            if (input.stateBuilderFactory.isPresent()) {
                stateBuilder = input.stateBuilderFactory.get().createStateBuilder();
                MetricListener metricListener = new MetricListener(conf, jpf, stateBuilder);
                jpf.addListener(metricListener);
            }

            // reset last observed cost before each execution.
            Observations.reset();

            ConcreteInput2TrieListener trieListener = new ConcreteInput2TrieListener(conf, jpf, trie, stateBuilder,
                    originalFileName, input.useUserDefinedCost);
            jpf.addListener(trieListener);

            jpf.run();

            if (jpf.foundErrors()) {
                System.out.println("#FOUND ERRORS = " + jpf.getSearchErrors().size());
            }

            if (spfMode.equals(ConcreteSPFMode.EXPORT)) {
                if (trieListener.didExposeNewBranch() || trieListener.didObserveBetterScore()) {
                    String outputfile = input.exportDir + "/id:" + String.format("%06d", lastId.incrementAndGet());

                    File tmpFile = new File(originalFileName);
                    File newFile = new File(outputfile);
                    tmpFile.renameTo(newFile);

                    String statistics = (System.currentTimeMillis() / 1000L) + "," + originalFileName + "," + outputfile
                            + (trieListener.didExposeNewBranch() ? ",branch" : "")
                            + (trieListener.didObserveBetterScore()
                                    ? (trie.getCostStrategy().equals(CostStrategy.MAXIMIZE) ? ",highscore"
                                            : ",lowscore") + "," + trieListener.getObservedCostForLeafNode()
                                    : "")
                            + "\n";
                    Statistics.appendExportStatistics(input, statistics);
                }
            }
            if (spfMode.equals(ConcreteSPFMode.IMPORT)) {
                String statistic = (System.currentTimeMillis() / 1000L) + "," + originalFileName + ","
                        + trieListener.getObservedCostForLeafNode()
                        + (trieListener.didObserveBetterScore()
                                ? (trie.getCostStrategy().equals(CostStrategy.MAXIMIZE) ? ",highscore" : ",lowscore")
                                : "")
                        + "\n";
                Statistics.appendImportStatistics(input, statistic);
            }

            this.trie = trieListener.getResultingTrie();

            return new Pair<>(trieListener.getObservedCostForLeafNode(), trieListener.didObserveBetterScore());

        } catch (JPFConfigException cx) {
            cx.printStackTrace();
        } catch (JPFException jx) {
            jx.printStackTrace();
        }

        return null;
    }

    private void runJPF(Trie trie, int additionalDecisions, int inputSize) {
        TrieGuidanceListener trieBuilderListener = null;
        try {
            Config conf = initSPFConfig();
            conf.setProperty("symbolic.collect_constraints", "false");

            input.symMaxInt.ifPresent(value -> conf.setProperty("symbolic.max_int", value));
            input.symMinInt.ifPresent(value -> conf.setProperty("symbolic.min_int", value));
            input.symMaxChar.ifPresent(value -> conf.setProperty("symbolic.max_char", value));
            input.symMinChar.ifPresent(value -> conf.setProperty("symbolic.min_char", value));
            input.symMaxByte.ifPresent(value -> conf.setProperty("symbolic.max_byte", value));
            input.symMinByte.ifPresent(value -> conf.setProperty("symbolic.min_byte", value));

            conf.setProperty("target.args", input.jpf_argument.replace("@@", ""));

            JPF jpf = new JPF(conf);

            SymbolicListener symbolicListener = new SymbolicListener(conf, jpf);
            jpf.addListener(symbolicListener);

            trieBuilderListener = new TrieGuidanceListener(conf, jpf, trie, additionalDecisions, pcAndSolutionQueue);
            jpf.addListener(trieBuilderListener);

            // reset last observed cost before each execution.
            Observations.reset();

            // Set the correct input size for the node of interest (only important for side-channel analysis).
            if (inputSize > -1) {
                Observations.lastObservedInputSize = inputSize;
            } else {
                Observations.lastObservedInputSize = input.inputSizes[0]; // set maximum
            }

            jpf.run();

            if (jpf.foundErrors()) {
                System.out.println("#FOUND ERRORS = " + jpf.getSearchErrors().size());
            }
        } catch (JPFConfigException cx) {
            cx.printStackTrace();
            System.exit(1);
        } catch (JPFException jx) {
            if (jx.getCause() instanceof ExitException) {
                if (((ExitException) jx.getCause()).shouldReport()) {
                    jx.printStackTrace();
                    System.exit(1);
                }
            } else {
                jx.printStackTrace();
                System.exit(1);
            }
        } catch (Exception e) {
            e.printStackTrace();
            System.exit(1);
        }

    }

    private Pair<Pair<PathCondition, Map<String, Object>>, Double> runJPF(String targetArgument,
            String originalFileName, ConcreteSPFMode spfMode) {

        if (targetArgument == null) {
            return null;
        }

        System.out.println("Run JPF with argument: " + targetArgument);

        try {
            Config conf = initSPFConfig();
            conf.setProperty("symbolic.collect_constraints", "true");
            // conf.setProperty("symbolic.dp", "no_solver"); // symcrete execution, no solver
            input.symMaxInt.ifPresent(value -> conf.setProperty("symbolic.max_int", value));
            input.symMinInt.ifPresent(value -> conf.setProperty("symbolic.min_int", value));
            input.symMaxChar.ifPresent(value -> conf.setProperty("symbolic.max_char", value));
            input.symMinChar.ifPresent(value -> conf.setProperty("symbolic.min_char", value));
            input.symMaxByte.ifPresent(value -> conf.setProperty("symbolic.max_byte", value));
            input.symMinByte.ifPresent(value -> conf.setProperty("symbolic.min_byte", value));

            conf.setProperty("target.args", input.jpf_argument.replace("@@", targetArgument));

            JPF jpf = new JPF(conf);

            SymbolicListener symbolicListener = new SymbolicListener(conf, jpf);
            jpf.addListener(symbolicListener);

            // reset last observed cost before each execution.
            Observations.lastObservedCost = 0.0;
            Observations.lastObservedSymbolicExpression = null;

            SymCreteCostListener symcreteListener = new SymCreteCostListener(conf, jpf);
            jpf.addListener(symcreteListener);

            jpf.run();

            if (jpf.foundErrors()) {
                System.out.println("#FOUND ERRORS = " + jpf.getSearchErrors().size());
            }

            Double observedCost = symcreteListener.getObservedFinalCost();
            PathCondition observedPC = symcreteListener.getObservedPathCondition();
            Map<String, Object> observedSolution = symcreteListener.getObservedPCSolution();

            return new Pair<>(new Pair<>(observedPC, observedSolution), observedCost);

        } catch (JPFConfigException cx) {
            cx.printStackTrace();
        } catch (JPFException jx) {
            jx.printStackTrace();
        }

        return null;
    }

    private Config initSPFConfig() {
        Config conf = JPF.createConfig(new String[0]);
        conf.setProperty("classpath", input.jpf_classpath);
        conf.setProperty("target", input.jpf_targetClass);
        conf.setProperty("symbolic.method", input.spf_symbolicMethod);
        conf.setProperty("jvm.insn_factory.class", "gov.nasa.jpf.symbc.SymbolicInstructionFactory");
        conf.setProperty("vm.storage.class", "nil");
        conf.setProperty("symbolic.dp", input.spf_dp);
        conf.setProperty("symbolic.optimizechoices", "false");
        conf.setProperty("symbolic.debug", "false");
        return conf;
    }

    private void buildTrieFromProcessedInput(Map<String, String> parseInputs, ConcreteSPFMode spfMode) {
        if (parseInputs != null) {
            for (Entry<String, String> inputEntry : parseInputs.entrySet()) {
                String originalFileName = inputEntry.getKey();
                String processedFileName = inputEntry.getValue().replaceAll(",", "#");

                // If the optimization parameter is enabled, then first try to optimize the current file. This makes
                // only sense if we use a user-defined cost metric.
                if (input.spf_dp.endsWith("optimize") && input.useUserDefinedCost) {

                    // Make a dry (without changing anything from the trie).
                    Pair<Pair<PathCondition, Map<String, Object>>, Double> resultOriginalInput = runJPF(
                            processedFileName, originalFileName, spfMode);
                    Pair<PathCondition, Map<String, Object>> observedPcAndSolution = resultOriginalInput._1;
                    Double observedCostOriginalInput = resultOriginalInput._2;

                    if (observedPcAndSolution._1 == null || observedPcAndSolution._2 == null
                            || observedPcAndSolution._2.isEmpty()) {
                        // run is not complete, likely because of an exeption, i.e. there is nothing to optimize.
                        System.out.println(); // TODO YN in general, how should we handle exception, also if it is not
                                              // in userdefined mode?, for now I disabled this in the application code
                        throw new RuntimeException("incomplete run");
                    }

                    // Generate input for maximized cost.
                    String maximizedInputFile = generateTmpInputfile(observedPcAndSolution);
                    List<String> listToMatchAPI = new ArrayList<>();
                    listToMatchAPI.add(maximizedInputFile);
                    Map<String, String> processedVersionOfMaximizedInputFile = input.ioUtils
                            .processInput(listToMatchAPI);
                    String processedMaximizedInputFile = processedVersionOfMaximizedInputFile.get(maximizedInputFile);

                    // Perform real run with trie.
                    Pair<Double, Boolean> resultMaximizedInput = runJPF(processedMaximizedInputFile, maximizedInputFile,
                            this.trie, spfMode);
                    Double observedCostMaximizedInput = resultMaximizedInput._1;
                    Boolean maximizedCostTriggeredNewHighscore = resultMaximizedInput._2;

                    if (spfMode.equals(ConcreteSPFMode.IMPORT)) {
                        // If we import files from AFL, we may want to directly export a maximized version if it is also
                        // a new highscore.
                        if (observedCostMaximizedInput > observedCostOriginalInput
                                && maximizedCostTriggeredNewHighscore) {
                            String outputfile = input.exportDir + "/id:"
                                    + String.format("%06d", lastId.incrementAndGet());

                            File tmpFile = new File(maximizedInputFile);
                            File newFile = new File(outputfile);
                            tmpFile.renameTo(newFile);

                            String statistics = (System.currentTimeMillis() / 1000L) + "," + maximizedInputFile + ","
                                    + outputfile + ",highscore,maximized," + observedCostMaximizedInput + "\n";
                            Statistics.appendExportStatistics(input, statistics);
                        }
                    }
                } else {
                    // If we do not maximize any terms, then this represents the normal run.
                    runJPF(processedFileName, originalFileName, this.trie, spfMode);
                }

            }
        }
    }

}
