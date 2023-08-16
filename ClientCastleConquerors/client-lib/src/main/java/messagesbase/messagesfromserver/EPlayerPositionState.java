package messagesbase.messagesfromserver;

import javax.xml.bind.annotation.XmlEnum;

@XmlEnum
public enum EPlayerPositionState {
	NoPlayerPresent,
	EnemyPlayerPosition,
	MyPlayerPosition,
	BothPlayerPosition;

	public boolean representsMyPlayer() {
		return this == BothPlayerPosition || this == MyPlayerPosition;
	}
}
