package vector.service;

import org.deeplearning4j.models.word2vec.Word2Vec;
import org.springframework.web.bind.annotation.*;
import vector.VectorModelUtils;
import vector.fastText.FastTextModel;
import vector.glove.GloveModel;
import vector.word2vec.Word2VecModel;

import java.util.Collection;

/**
 * @author DANISH AHMED on 10/22/2018
 */
@RestController
//@RequestMapping("/api")
public class Controller {

    @PostMapping("/glove/{word}/{n}")
    public void gloveNearestWords(@PathVariable(value = "word") String word,
                                                @PathVariable(value = "n") int n) {
        Word2Vec glove = GloveModel.gloveInstance.glove;
        Collection<String> nearestWords = VectorModelUtils.nNearestWords(glove, word, n);
        System.out.println(nearestWords);
    }

    @PostMapping("/word2vec/{word}/{n}")
    public void word2vecNearestWords(@PathVariable(value = "word") String word,
                                                @PathVariable(value = "n") int n) {
        Word2Vec word2Vec = Word2VecModel.word2vecInstance.word2Vec;
        Collection<String> nearestWords = VectorModelUtils.nNearestWords(word2Vec, word, n);
        System.out.println(nearestWords);
    }

    @PostMapping("/fastText/{word}/{n}")
    public void fastTextNearestWords(@PathVariable(value = "word") String word,
                                                @PathVariable(value = "n") int n) {
        Word2Vec fastText = FastTextModel.fastTextInstance.fastText;
        Collection<String> nearestWords = VectorModelUtils.nNearestWords(fastText, word, n);
        System.out.println(nearestWords);
    }
}
