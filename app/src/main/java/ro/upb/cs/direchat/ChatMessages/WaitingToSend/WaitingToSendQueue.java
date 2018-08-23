package ro.upb.cs.direchat.ChatMessages.WaitingToSend;

import java.util.ArrayList;
import java.util.List;

/**
 * Clasa ce reprezinta un ArrayList de ArrayList-uri
 * Contine lista waitingToSend (lista din
 * {@link ro.upb.cs.direchat.ChatMessages.WaitingToSend.WaitingToSendListElement}
 * si logica de a extrage elemente
 */
public class WaitingToSendQueue {

    private final List<WaitingToSendListElement> waitingToSend;

    private static final WaitingToSendQueue instace = new WaitingToSendQueue();

    /**
     *  Metoda ce intoarce instanca clasei
     * @return intsance acestei clase
     */
    public static WaitingToSendQueue getInstace() { return instace; }

    /**
     * Constructor pivate
     */
    private WaitingToSendQueue() { waitingToSend = new ArrayList<>(); }

    public List<String> getWaitingToSendItemsList(int tabNumber) {

        //pentru a putea mapa tabNumber catre WaitingToSend
        //metoda foloseste tabNumber - 1
        if ((tabNumber -1 ) >= 0 && (tabNumber - 1) < waitingToSend.size() - 1) {

            //daca elementul este null setez WaitingSendListElement la tabNumber - 1
            if (waitingToSend.get(tabNumber - 1) == null) {
                waitingToSend.set((tabNumber - 1), new WaitingToSendListElement());
            }
            // daca elemtnul este != null atunci nu facem nimic,
            // probabil lista este pregatita si elementele probabil sunt gata
        }else{
                //daca tabNumber nu este valabil, i adauga un WaitingToSendListElement
                //la sfarsitul listei waitingToSend List
                waitingToSend.add((tabNumber - 1), new WaitingToSendListElement());
        }

        return waitingToSend.get((tabNumber - 1)).getWaitingToSendList();

        }
}
