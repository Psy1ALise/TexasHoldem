package pokerserver;

import java.util.ArrayList;
import java.util.Collections;
import java.util.List;
import java.util.Map;
import java.util.stream.Collectors;
import java.util.stream.IntStream;

public class HandEvaluator {

	private final List<Card> cards;

	public HandEvaluator(List<Card> cards) {
		if (cards.size() != 5) {
			throw new IllegalArgumentException("Must provide exactly 5 cards");
		}
		this.cards = new ArrayList<>(cards);
	}

	public boolean isRoyalFlush() {
		boolean isSameSuit = cards.stream().map(Card::getSuit).distinct().count() == 1;
		boolean hasRoyalCards = cards.stream().map(Card::getRank)
				.filter(r -> r == Rank.TEN || r == Rank.JACK || r == Rank.QUEEN || r == Rank.KING || r == Rank.ACE)
				.distinct().count() == 5;
		return isSameSuit && hasRoyalCards;
	}

	public boolean isStraightFlush() {
		return isStraight() && isFlush();
	}

	public boolean isFourOfAKind() {
		return cards.stream().map(Card::getRank).distinct().anyMatch(r -> Collections.frequency(cards, r) == 4);
	}

	public boolean isFullHouse() {
		Map<Rank, Long> freqMap = cards.stream().collect(Collectors.groupingBy(Card::getRank, Collectors.counting()));
		return freqMap.containsValue(3L) && freqMap.containsValue(2L);
	}

	public boolean isFlush() {
		return cards.stream().map(Card::getSuit).distinct().count() == 1;
	}

	public boolean isStraight() {
		List<Rank> ranks = cards.stream().map(Card::getRank).collect(Collectors.toList());
		Collections.sort(ranks);
		if (isConsecutive(ranks)) {
			return true;
		}
		// For the special case where Ace acts as 1
		else if (ranks.contains(Rank.ACE)) {
			List<Rank> ranksWithLowAce = new ArrayList<>(ranks);
			ranksWithLowAce.remove(Rank.ACE);
			ranksWithLowAce.add(0, Rank.ACE);
			return isConsecutive(ranksWithLowAce);
		}

		return false;
	}

	// For distinction of straight A2345 and TJQKA
	private boolean isConsecutive(List<Rank> ranks) {
		return IntStream.range(0, ranks.size() - 1)
				.allMatch(i -> ranks.get(i).ordinal() + 1 == ranks.get(i + 1).ordinal());
	}

	public boolean isThreeOfAKind() {
		return cards.stream().map(Card::getRank).distinct().anyMatch(r -> Collections.frequency(cards, r) == 3);
	}

	public boolean isTwoPair() {
		Map<Rank, Long> freqMap = cards.stream().collect(Collectors.groupingBy(Card::getRank, Collectors.counting()));
		return freqMap.values().stream().filter(val -> val == 2).count() == 2;
	}

	public boolean isPair() {
		return cards.stream().map(Card::getRank).distinct().anyMatch(r -> Collections.frequency(cards, r) == 2);
	}

	public HandRank evaluateHand() {
		if (isRoyalFlush()) {
			return HandRank.ROYAL_FLUSH;
		} else if (isStraightFlush()) {
			return HandRank.STRAIGHT_FLUSH;
		} else if (isFourOfAKind()) {
			return HandRank.FOUR_OF_A_KIND;
		} else if (isFullHouse()) {
			return HandRank.FULL_HOUSE;
		} else if (isFlush()) {
			return HandRank.FLUSH;
		} else if (isStraight()) {
			return HandRank.STRAIGHT;
		} else if (isThreeOfAKind()) {
			return HandRank.THREE_OF_A_KIND;
		} else if (isTwoPair()) {
			return HandRank.TWO_PAIR;
		} else if (isPair()) {
			return HandRank.PAIR;
		} else {
			return HandRank.HIGH_CARD;
		}
	}
}
