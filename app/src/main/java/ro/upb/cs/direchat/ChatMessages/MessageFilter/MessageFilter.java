package ro.upb.cs.direchat.ChatMessages.MessageFilter;

import java.util.ArrayList;
import java.util.List;
import java.util.Locale;

/**
 * Clasa cu un filtru simplu pentru mesaje
 */
public class MessageFilter {

    //lista cu toate mesajele(lowercase) ignorate
    private List<String> lowerCaseBlackList;
    private static final MessageFilter instance = new MessageFilter();

    public static MessageFilter getInstance() { return instance; }

    private MessageFilter() {
        lowerCaseBlackList = new ArrayList<>();

        //aici adaugam mesaje in blacklist
        lowerCaseBlackList.add("blacklist");
    }

    /**
     * Metoda ce verifica daca mesajul throw exception
     * @param message   Mesajul pe care il verificam
     * @return  true daca mesajul nu este valid altfel false
     * @throws MessageException daca mesaj este null; mesaj este prea scurt; mesaj contine cuvinte din blacklist
     */
    public boolean isFiltered(String message) throws MessageException {
        if (message == null)
            throw new MessageException(MessageException.Reason.NULLMESSAGE);

        if (message.length() <= 1) {
            throw new MessageException(MessageException.Reason.MESSAGETOSHORT);
        }

        String [] chunkList = message.toLowerCase(Locale.US).split(" ");
        for (int i = 0; i < chunkList.length; i++)
            if (lowerCaseBlackList.contains(chunkList[i])) {
                throw new MessageException(MessageException.Reason.MESSAGEBLACKLISTED);
            }

        return false;
    }

}
