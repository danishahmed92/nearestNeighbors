package vector;

import org.deeplearning4j.models.embeddings.loader.VectorsConfiguration;
import org.deeplearning4j.models.embeddings.loader.WordVectorSerializer;
import org.deeplearning4j.models.word2vec.Word2Vec;

import java.io.File;
import java.util.Collection;

/**
 * @author DANISH AHMED on 10/22/2018
 */
public class VectorModel {
    private Word2Vec vectorModel;

    public Word2Vec setVectorModel(String textModelFile) {
        vectorModel = readVectorModel(textModelFile);
        return vectorModel;
    }

    public Word2Vec setVectorModel(File modelFile) {
        vectorModel = readVectorModel(modelFile);
        return vectorModel;
    }

    private Word2Vec readVectorModel(File modelFilePath) {
        return WordVectorSerializer.readWord2VecModel(modelFilePath);
    }

    private Word2Vec readVectorModel(String modelFilePath) {
        return WordVectorSerializer.readWord2VecModel(modelFilePath);
    }
}
