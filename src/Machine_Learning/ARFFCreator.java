package Machine_Learning;

import java.io.File;
import java.io.IOException;

import weka.core.Instances;
import weka.core.converters.ArffSaver;
import weka.core.converters.CSVLoader;

/**
 * Creates the .arff file from .csv file, for WEKA use.
 * 
 * @author Karl Appel v1.0
 * @version 6/2012 v1.0
 */
public class ARFFCreator {
	public ARFFCreator(String csv, String arff) throws IOException {
		// load CSV
		CSVLoader loader = new CSVLoader();
		loader.setSource(new File(csv));
		Instances data = loader.getDataSet();

		// save ARFF
		ArffSaver saver = new ArffSaver();
		saver.setInstances(data);
		saver.setFile(new File(arff));
		saver.setDestination(new File(arff));
		saver.writeBatch();
	}
}