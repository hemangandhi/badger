package edu.cmu.sv.badger.io;

import java.io.FileNotFoundException;
import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Paths;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

import gov.nasa.jpf.symbc.Observations;
import gov.nasa.jpf.symbc.numeric.PathCondition;

public class DynamicIntArrayIOUtils extends IOUtils {

    public static final String ID = "dynamic-int-byte-array";
    
    public int maxN;
    public int numberOfVars;
    
    public DynamicIntArrayIOUtils(int N, int M) {
        this.maxN = N;
        this.numberOfVars = M;
    }

    @Override
    public Map<String, String> processInput(List<String> inputFiles) {
        // here we don't need to process anything
        Map<String, String> res = new HashMap<>();
        for (String inputfile : inputFiles) {
            res.put(inputfile, inputfile);
        }
        return res;
    }

    @Override
    public void generateInputFiles(PathCondition pc, Map<String, Object> solution, String outputFile) {
        generateByteArrayInput(pc, solution, outputFile);
    }

    private void generateByteArrayInput(PathCondition pc, Map<String, Object> solution, String outputFile) {
        
        int n = Math.min(Observations.lastObservedInputSize, maxN);
        
        byte[] data = new byte[n*numberOfVars];
        int data_counter = 0;
        for (int i=0; i<this.numberOfVars; i++) {
            for (int j = 0; j < n; j++) {
                Object value = solution.get("sym_" + i + "_" + j);
                int intValue;
                if (value == null) {
                    intValue = 0;
                } else {
                    intValue = Math.toIntExact((long) value);
                }
                data[data_counter++] = (byte) intValue;
            }
        }
       
        try {
            Files.write(Paths.get(outputFile), data);
        } catch (FileNotFoundException e) {
            e.printStackTrace();
        } catch (IOException e) {
            e.printStackTrace();
        }
    }

}
