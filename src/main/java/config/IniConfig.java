package config;

import org.ini4j.Ini;
import java.io.IOException;

public class IniConfig {
    public static IniConfig configInstance;

    static {
        try {
            configInstance = new IniConfig();
        } catch (IOException e) {
            e.printStackTrace();
        }
    }

    public String stopWords;
    public String wordNet;
    public String resultPath;

    public String word2vec;
    public String glove;
    public String fastText;
    public String numberBatch;

    public String propertyGlossW2V;
    public String propertyGlossGlove;
    public String propertyGlossFT;

    public String propertySynsetW2V;
    public String propertySynsetGlove;
    public String propertySynsetFT;

    /**
     * reading configuration from sameAs.ini
     * and set variables that are globally required
     * @throws IOException
     */
    private IniConfig() throws IOException {
        String CONFIG_FILE = "systemConfig.ini";
        Ini configIni = new Ini(IniConfig.class.getClassLoader().getResource(CONFIG_FILE));

        stopWords = configIni.get("data", "stopWords");
        wordNet = configIni.get("data", "wordNet");
        resultPath = configIni.get("data", "resultPath");

        word2vec = configIni.get("sourceModel", "word2vec");
        glove = configIni.get("sourceModel", "glove");
        fastText = configIni.get("sourceModel", "fastText");
        numberBatch = configIni.get("sourceModel", "numberBatch");

        propertyGlossW2V = configIni.get("generatedModel", "propertyGlossW2V");
        propertyGlossGlove = configIni.get("generatedModel", "propertyGlossGlove");
        propertyGlossFT = configIni.get("generatedModel", "propertyGlossFT");

        propertySynsetW2V = configIni.get("generatedModel", "propertySynsetW2V");
        propertySynsetGlove = configIni.get("generatedModel", "propertySynsetGlove");
        propertySynsetFT = configIni.get("generatedModel", "propertySynsetFT");
    }
}
