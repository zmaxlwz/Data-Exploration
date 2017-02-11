package tuplereader;

import java.io.BufferedReader;
import java.io.FileNotFoundException;
import java.io.FileReader;
import java.io.IOException;
import java.util.Date;
import java.util.NoSuchElementException;
import java.util.StringTokenizer;

import middleware.datastruc.DenseInstanceWithID;
import weka.core.Instance;

public class FileTupleReader<PK> implements TupleReader<PK> {// one line per tuple
	
	BufferedReader br;
	public FileTupleReader(String fileName) throws FileNotFoundException {
		br = new BufferedReader(new FileReader(fileName));
	}

	@Override
	public Instance getNext() {
		String nextLine = null;
		try {
			nextLine = br.readLine();
			if(nextLine==null) {
				return null;
			}
			nextLine = nextLine.trim();
			if(!nextLine.startsWith("(") && !nextLine.startsWith("-") && !nextLine.startsWith("obj")) {
				StringTokenizer st = new StringTokenizer(nextLine, " |");
				int numAttrs = st.countTokens()-1;
				Instance sample = new DenseInstanceWithID(numAttrs,Long.valueOf(st.nextToken()));
				for(int i=0; i<numAttrs; i++) {
					sample.setValue(i, Double.valueOf(st.nextToken()));
				}
				return sample;
			}
			else {
				return getNext();
			}
		} catch (NoSuchElementException e1) {
			System.err.println((new Date(System.currentTimeMillis())).toString());
			System.err.println(nextLine);
			e1.printStackTrace();
		} catch (IOException e) {
			System.err.println((new Date(System.currentTimeMillis())).toString());
			e.printStackTrace();
		}
		return null;
	}

	@Override
	public void reset() {
		// TODO Auto-generated method stub
		
	}

	@Override
	public Instance getTuple(Object keyValue) {
		// TODO Auto-generated method stub
		return null;
	}

}
