package pokerserver;

import java.util.ArrayList;
import java.util.List;

public class PokerHandResolver {

    public HandRank getBestRank(List<Card> sevenCards) {
        List<List<Card>> combinations = generateCombinations(sevenCards, 5);

        HandRank bestHandRank = null;
        List<Card> bestHandCards = null;

        for (List<Card> combination : combinations) {
            HandEvaluator evaluator = new HandEvaluator(combination);
            HandRank currentRank = evaluator.evaluateHand();

            if (bestHandRank == null || currentRank.compareTo(bestHandRank) > 0) {
                bestHandRank = currentRank;
                bestHandCards = combination;
            }
        }

        return bestHandRank;
    }

    private List<List<Card>> generateCombinations(List<Card> cards, int k) {
        List<List<Card>> combinations = new ArrayList<>();
        generateCombinations(cards, k, 0, new ArrayList<>(), combinations);
        return combinations;
    }

    private void generateCombinations(List<Card> cards, int k, int start, List<Card> current, List<List<Card>> combinations) {
        if (k == 0) {
            combinations.add(new ArrayList<>(current));
            return;
        }

        for (int i = start; i < cards.size(); i++) {
            current.add(cards.get(i));
            generateCombinations(cards, k - 1, i + 1, current, combinations);
            current.remove(current.size() - 1);
        }
    }

	
}