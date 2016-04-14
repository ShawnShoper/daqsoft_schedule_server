package com.daqsoft.schedule.server.module.consensus;

import java.io.BufferedReader;
import java.io.IOException;
import java.io.InputStreamReader;
import java.util.Collection;
import java.util.HashSet;
import java.util.Set;
import java.util.Timer;
import java.util.TimerTask;
import java.util.concurrent.ConcurrentLinkedDeque;
import java.util.concurrent.TimeUnit;

import javax.annotation.PostConstruct;
import javax.annotation.PreDestroy;

import org.apache.hadoop.fs.FSDataInputStream;
import org.apache.hadoop.fs.FSDataOutputStream;
import org.apache.hadoop.fs.FileSystem;
import org.apache.hadoop.fs.Path;
import org.shoper.bloomfilter.BloomFilter;
import org.shoper.util.StringUtil;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Component;

import com.daqsoft.schedule.conf.ApplicationInfo;
import com.daqsoft.schedule.module.StartableModule;
import com.daqsoft.schedule.server.module.HDFSModule;

@Component
public class ConsensusHelper extends StartableModule
{
	@Autowired
	HDFSModule HDFSModule;
	@Autowired
	ConsensusStore consensusStore;
	@Autowired
	ApplicationInfo appInfo;
	Timer timer;
	final String POSDIR = "/user/cloudera/POS/";
	final String FILE_NEUTRAL = POSDIR + "neut-ral.dic";
	final String FILE_COMPLIMENTARY = POSDIR + "complimentary.dic";
	final String FILE_PEJORATIVE = POSDIR + "pejorative.dic";

	@PostConstruct
	public void init()
	{

		timer = new Timer(true);
		timer.schedule(new TimerTask() {
			@Override
			public void run()
			{
				try
				{
					writeFile();
				} catch (IllegalArgumentException e)
				{
					e.printStackTrace();
				} catch (IOException e)
				{
					e.printStackTrace();
				}
			}
		}, TimeUnit.SECONDS.toMillis(10), TimeUnit.SECONDS.toMillis(10));
	}
	@PreDestroy
	public void destory()
	{
		if (timer != null)
			timer.cancel();

	}
	public void writeFile() throws IllegalArgumentException, IOException
	{
		writeHDFSFile(consensusStore.getNeutralConsensuses(),
				new Path(FILE_NEUTRAL));
		writeHDFSFile(consensusStore.getPejorativeConsensuses(),
				new Path(FILE_PEJORATIVE));
		writeHDFSFile(consensusStore.getComplimentaryConsensuses(),
				new Path(FILE_COMPLIMENTARY));

	}
	void writeHDFSFile(ConcurrentLinkedDeque<String> queue, Path filePath)
			throws IOException
	{
		if (queue.isEmpty())
			return;
		FileSystem fs;
		fs = HDFSModule.getFileSystem();
		FSDataOutputStream out = fs.append(filePath);
		while (!queue.isEmpty())
		{
			String word = queue.poll();
			if (StringUtil.isEmpty(word))
				continue;
			out.write((word + "\n").getBytes());
		}
		out.flush();
		out.close();
	}
	public void importWords(String filePath) throws IOException
	{
		if (StringUtil.isEmpty(filePath))
			throw new IllegalArgumentException("filePath can not be empty...");
		String word = readFile(new Path(filePath));
		Set<String> words = StringUtil.splitStrNoneRepeat(word, "\\t");
		if (words.isEmpty())
			return;
		pushWords(words);
	}
	BloomFilter bloomFilter = new BloomFilter();
	/**
	 * Initial dic<br>
	 * Created by ShawnShoper Apr 7, 2016
	 * 
	 * @throws IllegalArgumentException
	 * @throws IOException
	 */
	private void readExistsWord() throws IllegalArgumentException, IOException
	{
		Set<String> sens = new HashSet<>();
		sens.addAll(StringUtil.splitStrNoneRepeat(
				readFile(new Path(FILE_COMPLIMENTARY)), "\\n"));
		sens.addAll(StringUtil
				.splitStrNoneRepeat(readFile(new Path(FILE_NEUTRAL)), "\\n"));
		sens.addAll(StringUtil.splitStrNoneRepeat(
				readFile(new Path(FILE_PEJORATIVE)), "\\n"));
		for (String sen : sens)
			bloomFilter.add(sen);
	}

	private String readFile(Path filePath) throws IOException
	{
		StringBuilder content = new StringBuilder();
		FileSystem fs = HDFSModule.getFileSystem();
		FSDataInputStream in = fs.open(filePath);
		// empty file...
		if (in.available() == 0)
			return null;
		BufferedReader d = new BufferedReader(new InputStreamReader(in));
		// Words has just one line and split by tab('\t')
		for (;;)
		{
			String line = d.readLine();
			if (line == null)
				break;
			content.append(line + "\n");
		}
		return content.toString();
	}
	public void pushWords(Collection<String> collections)
	{
		for (String word : collections)
			pushWord(word);
	}
	public void pushWord(String word)
	{
		if (!bloomFilter.exist(word))
			consensusStore.pushWord(word);
	}
	/**
	 * Get remain count for need be analyzed
	 * 
	 * @return
	 */
	public int getRemainCount()
	{
		return consensusStore.getRemainCount();
	}
	/**
	 * remove word if the word has be analyzed
	 * 
	 * @param word
	 */
	public void removeWord(String word)
	{
		consensusStore.remove(word);
	}
	public String poll()
	{
		return consensusStore.pollWord();
	}
	public void prepareToWrite(String keyword, int chararct, String ip)
	{
		switch (chararct)
		{
			case 0 :
				consensusStore.addComplimentary(keyword);
				break;
			case 1 :
				consensusStore.addNeutral(keyword);
				break;
			case 2 :
				consensusStore.addPejorative(keyword);
				break;
			default :
				break;
		}
		bloomFilter.add(keyword);

	}
	/**
	 * 如果队列空了，集合中还剩余有，那么则是跳过的，再把集合中的导入到队列 <br>
	 * Created by ShawnShoper Apr 7, 2016
	 */
	public void C2Q()
	{
		consensusStore.C2Q();
	}
	@Override
	public int start()
	{
		try
		{
			readExistsWord();
		} catch (Exception e1)
		{
			e1.printStackTrace();
		}
		return 0;
	}
	@Override
	public void stop()
	{
	}
}
