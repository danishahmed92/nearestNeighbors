package vector.fastText;

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
public class FastTextModel extends VectorModel {
    public static FastTextModel fastTextInstance;
    static {
        try {
            fastTextInstance = new FastTextModel();
        } catch (IOException e) {
            e.printStackTrace();
        }
    }

    public Word2Vec fastText;
    public FastTextModel() throws FileNotFoundException {
//        File filePath = new ClassPathResource("wiki-news-300d-1M.vec").getFile();
//        FastText = setVectorModel(filePath);

        fastText = setVectorModel(IniConfig.configInstance.fastText);
    }
}
