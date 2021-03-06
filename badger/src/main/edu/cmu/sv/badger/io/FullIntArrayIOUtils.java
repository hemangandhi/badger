package edu.cmu.sv.badger.io;

import java.io.File;
import java.io.FileNotFoundException;
import java.io.IOException;
import java.nio.ByteBuffer;
import java.nio.file.Files;
import java.nio.file.Paths;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

import gov.nasa.jpf.symbc.numeric.PathCondition;

public class FullIntArrayIOUtils extends IOUtils {

    public static final String ID = "full-int-byte-array";

    public int N;

    public FullIntArrayIOUtils(int N) {
        this.N = N;
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
        byte[] data = new byte[N * Integer.BYTES];
        int dataCounter = 0;
        for (int i = 0; i < N; i++) {

            // Get int value from model.
            Object value = solution.get("sym_" + i);
            int intValue;
            if (value == null) {
                intValue = 0;
            } else {
                intValue = Math.toIntExact((long) value);
            }

            // Transform int in byte[].
            ByteBuffer bb = ByteBuffer.allocate(Integer.BYTES);
            bb.putInt(intValue);
            byte[] byteArray = bb.array();

            // Save byte values in data[].
            data[dataCounter++] = byteArray[0];
            data[dataCounter++] = byteArray[1];
            data[dataCounter++] = byteArray[2];
            data[dataCounter++] = byteArray[3];

        }
        try {
            Files.write(Paths.get(outputFile), data);
        } catch (FileNotFoundException e) {
            e.printStackTrace();
        } catch (IOException e) {
            e.printStackTrace();
        }
    }

    public static void main(String[] args) {

//        String inputFileName = "/Users/yannic/repositories/cmu/memoise/issta-experiments/07-Category1_not_vulnerable/kelinci_analysis/in_dir/example.txt";
//        String outputFileName = "/Users/yannic/repositories/cmu/memoise/issta-experiments/07-Category1_not_vulnerable/kelinci_analysis/in_dir/example-byte.txt";

//        String content;
        try {
//            content = new String(Files.readAllBytes(Paths.get(inputFileName)));
//            content = content.trim();
            
            int[] array = {10,9,8,7,6,5,4,3,2,1};
            int[] array2 = {1,2,3,4,5,6,7,8,9,10};
            
            int N = 10;
            byte[] data = new byte[N * Integer.BYTES];
            int dataCounter = 0;
            for (int i=0; i<N; i++) {
                
                int value = array2[i];
                
                // Transform int in byte[].
                ByteBuffer bb = ByteBuffer.allocate(Integer.BYTES);
                bb.putInt(value);
                byte[] byteArray = bb.array();
                
                // Save byte values in data[].
                data[dataCounter++] = byteArray[0];
                data[dataCounter++] = byteArray[1];
                data[dataCounter++] = byteArray[2];
                data[dataCounter++] = byteArray[3];
                
            }
            
            File outputFile = new File("/Users/yannic/repositories/cmu/memoise/issta-experiments/example2.txt");
            outputFile.createNewFile();
            Files.write(outputFile.toPath(), data);

        } catch (IOException e) {
            e.printStackTrace();
        }

    }

}
