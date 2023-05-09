package pokerserver;

import java.util.ArrayList;
import java.util.Collections;
import java.util.List;
import java.util.stream.Collectors;
import java.util.stream.IntStream;

import pokerserver.Card.Rank;

public class HandEvaluator {

	private final List<Card> cards;

	public HandEvaluator(List<Card> cards) {
		if (cards.size() != 7) {
			throw new IllegalArgumentException("Must provide exactly 7 cards");
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
		boolean isSameSuit = cards.stream().map(Card::getSuit).distinct().count() == 1;
		boolean isConsecutive = IntStream.range(0, cards.size() - 1)
				.allMatch(i -> cards.get(i).getRank().ordinal() + 1 == cards.get(i + 1).getRank().ordinal());
		return isSameSuit && isConsecutive;
	}

	public boolean isFourOfAKind() {
		return cards.stream().map(Card::getRank).distinct()
				.anyMatch(r -> Collections.frequency(cards, new Card(null, r)) == 4);
	}

	public boolean isFullHouse() {
		Map<Rank, Long> freqMap = cards.stream().collect(Collectors.groupingBy(Card::getRank, Collectors.counting()));
		return freqMap.containsValue(3L) && freqMap.containsValue(2L);
	}

	public boolean isFlush() {
		return cards.stream().map(Card::getSuit).distinct().count() == 1;
	}

	public boolean isStraight() {
		return IntStream.range(0, cards.size() - 1)
				.allMatch(i -> cards.get(i).getRank().ordinal() + 1 == cards.get(i + 1).getRank().ordinal());
	}

	public boolean isThreeOfAKind() {
		return cards.stream().map(Card::getRank).distinct()
				.anyMatch(r -> Collections.frequency(cards, new Card(null, r)) == 3);
	}

	public boolean isTwoPair() {
		List<Rank> ranks = cards.stream().map(Card::getRank).distinct().collect(Collectors.toList());
		return ranks.size() == 3 && Collections.frequency(cards, new Card(null, ranks.get(0))) == 2
				&& Collections.frequency(cards, new Card(null, ranks.get(1))) == 2;
	}

	public boolean isPair() {
		return cards.stream().map(Card::getRank).distinct()
				.anyMatch(r -> Collections.frequency(cards, new Card(null, r)) == 2);
	}

	public Card highCard() {
		return cards.stream().max(Comparator.comparing(Card::getRank)).orElse(null);
	}

	public HandRank evaluateHand() {
		HandRank bestRank = HandRank.HIGH_CARD;
		List<Card> bestHand = new ArrayList<>();

		// Generate all possible combinations of 5 cards from the 7 cards
		List<List<Card>> combinations = generateCombinations(cards, 5);

		// Evaluate the rank of each combination and find the best one
		for (List<Card> hand : combinations) {
			HandRank rank = evaluateHand(hand);
			if (rank.ordinal() > bestRank.ordinal()) {
				bestRank = rank;
				bestHand = hand;
			}
		}

		return new HandRank(bestRank, bestHand);
	}

	private HandRank evaluateHand(List<Card> hand) {
		if (isRoyalFlush(hand)) {
			return HandRank.ROYAL_FLUSH;
		} else if (isStraightFlush(hand)) {
			return HandRank.STRAIGHT_FLUSH;
		} else if (isFourOfAKind(hand)) {
			return HandRank.FOUR_OF_A_KIND;
		} else if (isFullHouse(hand)) {
			return HandRank.FULL_HOUSE;
		} else if (isFlush(hand)) {
			return HandRank.FLUSH;
		} else if (isStraight(hand)) {
			return HandRank.STRAIGHT;
		} else if (isThreeOfAKind(hand)) {
			return HandRank.THREE_OF_A_KIND;
		} else if (isTwoPair(hand)) {
			return HandRank.TWO_PAIR;
		} else if (isPair(hand)) {
			return HandRank.PAIR;
		} else {
			return HandRank.HIGH_CARD;
		}
	}

	private List<List<Card>> generateCombinations(List<Card> cards, int k) {
		List<List<Card>> result = new ArrayList<>();
		int[] indices = new int[k];
		for (int i = 0; i < k; i++) {
			indices[i] = i;
		}

		while (indices[0] <= cards.size() - k) {
			List<Card> combination = new ArrayList<>(k);
			for (int i = 0; i < k; i++) {
				combination.add(cards.get(indices[i]));
			}
			result.add(combination);

			int i = k - 1;
			while (i >= 0 && indices[i] == cards.size() - k + i) {
				i--;
			}
			if (i < 0) {
				break;
			}
			indices[i]++;
			for (int j = i + 1; j < k; j++) {
				indices[j] = indices[i] + j - i;
			}
		}

		return result;
	}

}
