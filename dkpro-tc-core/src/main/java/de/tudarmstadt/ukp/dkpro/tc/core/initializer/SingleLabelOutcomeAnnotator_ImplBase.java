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
package de.tudarmstadt.ukp.dkpro.tc.core.initializer;

import org.apache.uima.analysis_engine.AnalysisEngineProcessException;
import org.apache.uima.collection.CollectionException;
import org.apache.uima.fit.component.JCasAnnotator_ImplBase;
import org.apache.uima.jcas.JCas;

import de.tudarmstadt.ukp.dkpro.tc.api.type.TextClassificationOutcome;

public abstract class SingleLabelOutcomeAnnotator_ImplBase
	extends JCasAnnotator_ImplBase
	implements SingleLabelOutcomeAnnotator
{
 
	@Override
	public void process(JCas jcas)
			throws AnalysisEngineProcessException
	{
	    TextClassificationOutcome outcome = new TextClassificationOutcome(jcas);
	    outcome.setOutcome(getTextClassificationOutcome(jcas));
	    outcome.setWeight(getTextClassificationOutcomeWeight(jcas));
	    outcome.addToIndexes();
	}

	/**
	 * This methods adds a (default) weight to instances. Readers which assign specific weights to
	 * instances need to override this method.
	 * 
	 * @param jcas
	 *            the JCas to add the annotation to
	 * @return a double between zero and one
	 * @throws CollectionException
	 */
	public double getTextClassificationOutcomeWeight(JCas jcas)
			throws AnalysisEngineProcessException
	{
		return 1.0;
	}
}
