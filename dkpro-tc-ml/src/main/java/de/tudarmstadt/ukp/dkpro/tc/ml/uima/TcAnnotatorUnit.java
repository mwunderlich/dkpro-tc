/*******************************************************************************
 * Copyright 2015
 * Ubiquitous Knowledge Processing (UKP) Lab
 * Technische Universität Darmstadt
 * 
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 * 
 *   http://www.apache.org/licenses/LICENSE-2.0
 * 
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 ******************************************************************************/

package de.tudarmstadt.ukp.dkpro.tc.ml.uima;

import java.io.File;
import java.util.Arrays;
import java.util.List;

import org.apache.log4j.Logger;
import org.apache.uima.UimaContext;
import org.apache.uima.analysis_engine.AnalysisEngine;
import org.apache.uima.analysis_engine.AnalysisEngineDescription;
import org.apache.uima.analysis_engine.AnalysisEngineProcessException;
import org.apache.uima.fit.component.JCasAnnotator_ImplBase;
import org.apache.uima.fit.descriptor.ConfigurationParameter;
import org.apache.uima.fit.factory.AnalysisEngineFactory;
import org.apache.uima.fit.util.JCasUtil;
import org.apache.uima.jcas.JCas;
import org.apache.uima.resource.ExternalResourceDescription;
import org.apache.uima.resource.ResourceInitializationException;

import de.tudarmstadt.ukp.dkpro.tc.api.type.TextClassificationOutcome;
import de.tudarmstadt.ukp.dkpro.tc.api.type.TextClassificationUnit;
import de.tudarmstadt.ukp.dkpro.tc.core.Constants;
import de.tudarmstadt.ukp.dkpro.tc.core.ml.ModelSerialization_ImplBase;
import de.tudarmstadt.ukp.dkpro.tc.core.ml.TCMachineLearningAdapter;
import de.tudarmstadt.ukp.dkpro.tc.fstore.simple.DenseFeatureStore;
import de.tudarmstadt.ukp.dkpro.tc.ml.modelpersist.ModelPersistUtil;

/**
 * Applies the given model to all TC focus annotated sections of the CAS.
 * Note that this Annotator is assumed to be used after the CAS multiplication
 * step in a Unit classification setting. Also, TC units and dummy TC outcomes
 * must have been added to the CASes already in some pipepline step prior to the
 * one implemented with the TcAnnotatorUnit instance.
 * 
 * @author Martin Wunderlich (martin@wunderlich.com)
 * (based on TcAnnotatorSequence)
 *
 */
public class TcAnnotatorUnit extends JCasAnnotator_ImplBase {

    public static final String PARAM_TC_MODEL_LOCATION = "tcModel";
    @ConfigurationParameter(name = PARAM_TC_MODEL_LOCATION, mandatory = true)
    protected File tcModelLocation;

    private String learningMode = Constants.LM_SINGLE_LABEL;
    private String featureMode = Constants.FM_UNIT;

    private List<String> featureExtractors;
    private List<Object> parameters;

    private TCMachineLearningAdapter mlAdapter;

    private AnalysisEngine engine;

    @Override
    public void initialize(UimaContext context)
        throws ResourceInitializationException
    {
        super.initialize(context);

        try {
            mlAdapter = ModelPersistUtil.initMachineLearningAdapter(tcModelLocation);
            parameters = ModelPersistUtil.initParameters(tcModelLocation);
            featureExtractors = ModelPersistUtil.initFeatureExtractors(tcModelLocation);
            
            AnalysisEngineDescription connector = getSaveModelConnector(parameters,
                    tcModelLocation.getAbsolutePath(), mlAdapter.getDataWriterClass().toString(),
                    learningMode, featureMode, DenseFeatureStore.class.getName(),
                    featureExtractors.toArray(new String[0]));
           
            engine = AnalysisEngineFactory.createEngine(connector);
        }
        catch (Exception e) {
            throw new ResourceInitializationException(e);
        }
    }

    @Override
    public void process(JCas jcas)
        throws AnalysisEngineProcessException
    {
    	Logger.getLogger(getClass()).debug("START: process(JCAS)");
    	
    	// Verify that Unit and Outcome annotations are already present
    	if( JCasUtil.select(jcas, TextClassificationOutcome.class).size() < 1 )
    		throw new IllegalArgumentException("At least one TC outcome annotation must be present in the CAS.");
    	if( JCasUtil.select(jcas, TextClassificationUnit.class).size() < 1 )
    		throw new IllegalArgumentException("At least one TC unit annotation must be present in the CAS.");
    	
    	// Process and classify
        try {
            engine.process(jcas);
        }
        catch (Exception e) {
            throw new AnalysisEngineProcessException(e);
        }
        
        Logger.getLogger(getClass()).debug("FINISH: process(JCAS)");
    }


    /**
     * @param featureExtractorClassNames
     * @return A fully configured feature extractor connector
     * @throws ResourceInitializationException
     */
    private AnalysisEngineDescription getSaveModelConnector(List<Object> parameters,
            String outputPath, String dataWriter, String learningMode, String featureMode,
            String featureStore, String... featureExtractorClassNames)
        throws ResourceInitializationException
    {
        // convert parameters to string as external resources only take string parameters
    	List<Object> convertedParameters = TcAnnotatorUtil.convertParameters(parameters);
        
    	List<ExternalResourceDescription> extractorResources = TcAnnotatorUtil.loadExternalResourceDescriptionOfFeatures(
				outputPath, featureExtractorClassNames, convertedParameters);

        // add the rest of the necessary parameters with the correct types
        parameters.addAll(Arrays.asList(TcAnnotatorUnit.PARAM_TC_MODEL_LOCATION,
                tcModelLocation, ModelSerialization_ImplBase.PARAM_OUTPUT_DIRECTORY, outputPath,
                ModelSerialization_ImplBase.PARAM_DATA_WRITER_CLASS, dataWriter,
                ModelSerialization_ImplBase.PARAM_LEARNING_MODE, learningMode,
                ModelSerialization_ImplBase.PARAM_FEATURE_EXTRACTORS, extractorResources,
                ModelSerialization_ImplBase.PARAM_FEATURE_FILTERS, null,
                ModelSerialization_ImplBase.PARAM_IS_TESTING, true,
                ModelSerialization_ImplBase.PARAM_FEATURE_MODE, featureMode,
                ModelSerialization_ImplBase.PARAM_FEATURE_STORE_CLASS, featureStore));

        return AnalysisEngineFactory.createEngineDescription(
                mlAdapter.getLoadModelConnectorClass(), parameters.toArray());
    }
    

}
