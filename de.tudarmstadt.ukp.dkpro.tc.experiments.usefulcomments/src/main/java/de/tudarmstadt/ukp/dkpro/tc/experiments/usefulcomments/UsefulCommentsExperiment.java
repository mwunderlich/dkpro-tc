package de.tudarmstadt.ukp.dkpro.tc.experiments.usefulcomments;

import static org.apache.uima.fit.factory.AnalysisEngineFactory.createAggregateDescription;
import static org.apache.uima.fit.factory.AnalysisEngineFactory.createPrimitiveDescription;
import static org.apache.uima.fit.factory.CollectionReaderFactory.createDescription;

import java.io.File;
import java.io.IOException;
import java.util.ArrayList;
import java.util.List;

import net.sf.json.JSONObject;
import net.sf.json.JSONSerializer;

import org.apache.commons.io.FileUtils;
import org.apache.uima.analysis_engine.AnalysisEngineDescription;
import org.apache.uima.collection.CollectionReaderDescription;
import org.apache.uima.resource.ResourceInitializationException;

import de.tudarmstadt.ukp.dkpro.core.opennlp.OpenNlpPosTagger;
import de.tudarmstadt.ukp.dkpro.core.tokit.BreakIteratorSegmenter;
import de.tudarmstadt.ukp.dkpro.lab.Lab;
import de.tudarmstadt.ukp.dkpro.lab.task.ParameterSpace;
import de.tudarmstadt.ukp.dkpro.lab.task.impl.BatchTask.ExecutionPolicy;
import de.tudarmstadt.ukp.dkpro.tc.core.meta.MetaCollector;
import de.tudarmstadt.ukp.dkpro.tc.experiments.usefulcomments.io.CommentsReader;
import de.tudarmstadt.ukp.dkpro.tc.features.ngram.meta.NGramMetaCollector;
import de.tudarmstadt.ukp.dkpro.tc.weka.report.BatchOutcomeIDReport;
import de.tudarmstadt.ukp.dkpro.tc.weka.report.BatchTrainTestReport;
import de.tudarmstadt.ukp.dkpro.tc.weka.task.BatchTaskTrainTest;
import de.tudarmstadt.ukp.dkpro.tc.weka.writer.WekaDataWriter;

/**
 * This Java-based experiment setup of the SentimentPolarityExperiment loads all its configurations
 * from a json file using the ParameterSpaceParser. Alternatively, the parameters could be defined
 * directly in this class, which makes on-the-fly changes more difficult when the experiment is run
 * on a server.
 */
public class UsefulCommentsExperiment
{

    private String languageCode;
    private String corpusPath;
    private String annotationsPathTrain;
    private String annotationsPathTest;

    public static void main(String[] args)
        throws Exception
    {
        UsefulCommentsExperiment experiment = new UsefulCommentsExperiment();
        ParameterSpace pSpace = experiment.setup();

        experiment.runTrainTest(pSpace);
    }

    /**
     * Initialize Experiment
     * 
     * @return ParameterSpace for the javaExperiment
     * @throws Exception
     */
    protected ParameterSpace setup()
        throws Exception
    {
        String jsonPath = FileUtils.readFileToString(new File(
                "src/main/resources/config/train.json"));
        JSONObject json = (JSONObject) JSONSerializer.toJSON(jsonPath);

        languageCode = json.getString("languageCode");
        corpusPath = json.getString("corpusPath");
        annotationsPathTrain = json.getString("annotationsPathTrain");
        annotationsPathTest = json.getString("annotationsPathTest");

        return ParameterSpaceParser.createParamSpaceFromJson(json);
    }

    // ##### TRAIN-TEST #####
    protected void runTrainTest(ParameterSpace pSpace)
        throws Exception
    {

        BatchTaskTrainTest batch = new BatchTaskTrainTest("UsefulCommentsTrainTest",
                getReaderDesc(annotationsPathTrain, corpusPath, languageCode),
                getReaderDesc(annotationsPathTest, corpusPath, languageCode), getPreprocessing(), getMetaCollectors(),
                WekaDataWriter.class.getName());
        batch.setType("Evaluation-UsefulComments-TrainTest");
        batch.setParameterSpace(pSpace);
        batch.setExecutionPolicy(ExecutionPolicy.RUN_AGAIN);
        batch.addReport(BatchTrainTestReport.class);
        batch.addReport(BatchOutcomeIDReport.class);

        // Run
        Lab.getInstance().run(batch);
    }

    protected CollectionReaderDescription getReaderDesc(String annotationsPath, String corpusPath, String languageCode)
        throws ResourceInitializationException, IOException
    {

        return createDescription(CommentsReader.class,
        		CommentsReader.PARAM_CORPUS_PATH, corpusPath,
        		CommentsReader.PARAM_ANNOTATIONS_PATH, annotationsPath
                );
    }

    protected AnalysisEngineDescription getPreprocessing()
        throws ResourceInitializationException
    {

        return createAggregateDescription(
                createPrimitiveDescription(BreakIteratorSegmenter.class),
                createPrimitiveDescription(OpenNlpPosTagger.class, OpenNlpPosTagger.PARAM_LANGUAGE,
                        languageCode));
    }

    protected List<Class<? extends MetaCollector>> getMetaCollectors()
    {
        List<Class<? extends MetaCollector>> metaCollectors = new ArrayList<Class<? extends MetaCollector>>();
        metaCollectors.add(NGramMetaCollector.class);

        return metaCollectors;
    }
}