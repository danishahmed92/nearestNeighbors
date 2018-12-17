package utils;

import org.deeplearning4j.models.word2vec.Word2Vec;
import org.nd4j.linalg.api.ndarray.INDArray;
import vector.VectorModelUtils;

import java.io.FileNotFoundException;
import java.io.PrintWriter;
import java.io.UnsupportedEncodingException;
import java.util.Arrays;
import java.util.HashMap;
import java.util.List;

public class GenerateModel {
    private Word2Vec vecModel;
    private HashMap<String, List<String>> keyTextMap = new HashMap<>();

    public GenerateModel(Word2Vec vecModel, HashMap<String, List<String>> keyTextMap) {
        this.vecModel = vecModel;
        this.keyTextMap = keyTextMap;
    }

    /*
    * given a list of Strings against a key
    * it merge all the strings (repeated words)
    * and calculate mean vector of the strings
    * then write float values of mean against that key
    * */
    public void generateVectorModel (String outputFile) {
        try {
            PrintWriter writer = new PrintWriter(outputFile, "UTF-8");

            for (String vecId : keyTextMap.keySet()) {
                List<String> labelWordsForField = keyTextMap.get(vecId);
                String allLabelsL2 = String.join(" ", labelWordsForField);

                String[] splitLabels = allLabelsL2.split(" ");
                List<String> wordsForGettingMeanVec = Arrays.asList(splitLabels);
                INDArray meanVec = VectorModelUtils.getMeanVecFromWordList(vecModel, wordsForGettingMeanVec);

                StringBuilder vectorToWrite = new StringBuilder(vecId.replaceAll(" ", "_") + " ");
                for (float vecVal : meanVec.toFloatVector()) {
                    vectorToWrite.append(vecVal).append(" ");
                }
                vectorToWrite = new StringBuilder(vectorToWrite.toString().trim());
                writer.println(vectorToWrite);
            }
            writer.close();
        } catch (FileNotFoundException | UnsupportedEncodingException e) {
            e.printStackTrace();
        }
    }
}
