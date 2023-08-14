package messagesbase.messagesfromserver;

import javax.xml.bind.annotation.XmlEnum;

@XmlEnum
public enum EFortState {
	NoOrUnknownFortState,
	MyFortPresent,
	EnemyFortPresent;
}
