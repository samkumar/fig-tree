package figtree;

import java.util.HashMap;

public class LogFile {
	public LogFile() {
		bytes = new HashMap<Integer, Integer>();
	}
	
	/**
	 * Simulates a write on a log file.
	 * @param range The bytes to be written.
	 * @return The index of the log entry appended to the log.
	 */
	public synchronized int write(Interval range) {
		int entryIndex = bytes.size();
		int right = range.right();
		for (int i = range.left(); i < right; i++) {
			bytes.put(i, entryIndex);
		}
		return entryIndex;
	}
	
	/**
	 * Simulates a read on a log file.
	 * @param position The index of the byte to read.
	 * @return The index of the log entry containing the relevant data.
	 */
	public synchronized int read(int position) {
		return bytes.get(position);
	}
	
	private HashMap<Integer, Integer> bytes;
}
