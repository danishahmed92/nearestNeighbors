package vector.word2vec;

import config.IniConfig;
import org.datavec.api.util.ClassPathResource;
import org.deeplearning4j.models.word2vec.Word2Vec;
import vector.VectorModel;

import java.io.File;
import java.io.FileNotFoundException;
import java.io.IOException;

/**
 * @author DANISH AHMED on 10/22/2018
 */
public class Word2VecModel extends VectorModel {
    public static Word2VecModel word2vecInstance;
    static {
        try {
            word2vecInstance = new Word2VecModel();
        } catch (IOException e) {
            e.printStackTrace();
        }
    }
    public Word2Vec word2Vec;
    public Word2VecModel() throws FileNotFoundException {
//        File filePath = new ClassPathResource("GoogleNews-vectors-negative300.bin.gz").getFile();
//        word2Vec = setVectorModel(filePath);

        word2Vec = setVectorModel(IniConfig.configInstance.word2vec);
    }
}
