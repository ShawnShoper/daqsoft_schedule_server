package com.daqsoft.schedule.server.module.consensus;

import java.util.concurrent.ConcurrentLinkedDeque;

import org.shoper.concurrent.collection.ConcurrentSet;
import org.springframework.stereotype.Component;

@Component
public class ConsensusStore
{
	// Store parti-ciple words that need be analyzed
	private ConcurrentLinkedDeque<String> consensusQueue = new ConcurrentLinkedDeque<>();
	// store original word,Use for check submit.
	private ConcurrentSet<String> orginalConsensuses = new ConcurrentSet<>();
	/**
	 * 0.positive<br>
	 * 1.neutral<br>
	 * 2.Pejorative<br>
	 */
	private ConcurrentLinkedDeque<String> complimentaryConsensuses = new ConcurrentLinkedDeque<>();
	private ConcurrentLinkedDeque<String> neutralConsensuses = new ConcurrentLinkedDeque<>();
	private ConcurrentLinkedDeque<String> pejorativeConsensuses = new ConcurrentLinkedDeque<>();
	ConcurrentLinkedDeque<String> getComplimentaryConsensuses()
	{
		return complimentaryConsensuses;
	}
	ConcurrentLinkedDeque<String> getNeutralConsensuses()
	{
		return neutralConsensuses;
	}
	ConcurrentLinkedDeque<String> getPejorativeConsensuses()
	{
		return pejorativeConsensuses;
	}
	void addPejorative(String word)
	{
		pejorativeConsensuses.add(word);
	}
	void addNeutral(String word)
	{
		neutralConsensuses.add(word);
	}
	void addComplimentary(String word)
	{
		complimentaryConsensuses.add(word);
	}
	String pollWord()
	{
		String word = consensusQueue.poll();
		if (word == null)
		{
			C2Q();
			word = consensusQueue.poll();
		}
		return word;
	}
	void pushWord(String word)
	{
		if (!consensusQueue.contains(word))
			consensusQueue.addLast(word);
		if (!orginalConsensuses.contains(word))
			orginalConsensuses.add(word);
	}
	/**
	 * remove word
	 * 
	 * @param keyword
	 */
	void remove(String keyword)
	{
		if (orginalConsensuses.contains(keyword))
			orginalConsensuses.remove(keyword);
	}
	/**
	 * get remain count
	 * 
	 * @return remain count
	 */
	public int getRemainCount()
	{
		return orginalConsensuses.size();
	}
	public void C2Q()
	{
		if (getRemainCount() > 0)
		{
			for (String word : orginalConsensuses)
			{
				if (!consensusQueue.contains(word))
					consensusQueue.addLast(word);
			}
		}
	}
}
