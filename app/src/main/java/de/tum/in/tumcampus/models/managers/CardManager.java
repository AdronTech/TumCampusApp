package de.tum.in.tumcampus.models.managers;

import android.content.Context;

import java.util.ArrayList;
import java.util.List;

import de.tum.in.tumcampus.cards.Card;

public class CardManager {
    public static final int CARD_CAFETERIA = 1;
    public static final int CARD_TUITION_FEE = 2;

    private static List<Card> cards = new ArrayList<Card>();
    private static List<ProvidesCard> managers = new ArrayList<ProvidesCard>();

    public static void addCard(Card card) {
        cards.add(card);
    }

    public static int getCardCount() {
        return cards.size();
    }

    public static Card getCard(int pos) {
        return cards.get(pos);
    }


    /** HOWTO ADD A CARD
     * 1. let the manager class implement ProvidesCard
     * 2. Create a new class extending Card
     * 3. implement the getView method in this class
     * 4. create a new instance of this card in the OnUpdateCard of the manager
     * 5. add this card to the CardManager by calling addCard(card)
     * 6. add an instance of the manager class to the managers list below
     * */
    public static void update(Context context) {
        managers.add(new CafeteriaManager(context));
        managers.add(new TuitionFeeManager());

        for(ProvidesCard manager : managers)
            manager.OnUpdateCard(context);
    }
}
