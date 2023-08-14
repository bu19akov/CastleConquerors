package messagesbase.messagesfromserver;

import javax.xml.bind.annotation.XmlEnum;

@XmlEnum
public enum EPlayerGameState {
	Won,
	Lost,
	MustAct,
	MustWait;
}
