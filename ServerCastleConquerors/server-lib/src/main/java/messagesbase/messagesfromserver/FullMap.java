package messagesbase.messagesfromserver;

import java.util.Collection;
import java.util.Collections;
import java.util.HashSet;
import java.util.Iterator;
import java.util.Set;
import java.util.stream.Stream;

import javax.xml.bind.annotation.XmlAccessType;
import javax.xml.bind.annotation.XmlAccessorType;
import javax.xml.bind.annotation.XmlElement;
import javax.xml.bind.annotation.XmlElementWrapper;
import javax.xml.bind.annotation.XmlRootElement;

@XmlRootElement(name = "fullMap")
@XmlAccessorType(XmlAccessType.NONE)
public final class FullMap implements Iterable<FullMapNode> {

	@XmlElementWrapper(name = "mapNodes")
	@XmlElement(name = "mapNode")
	private final Set<FullMapNode> mapNodes = new HashSet<>();

	public FullMap() {

	}

	public FullMap(Collection<FullMapNode> mapNodes) {
		if (mapNodes != null && !mapNodes.isEmpty()) {
			this.mapNodes.addAll(mapNodes);
		}
	}

	public Collection<FullMapNode> getMapNodes() {
		return Collections.unmodifiableCollection(mapNodes);
	}

	public boolean isEmpty() {
		return mapNodes.isEmpty();
	}

	public Stream<FullMapNode> stream() {
		return mapNodes.stream();
	}

	@Override
	public Iterator<FullMapNode> iterator() {
		return getMapNodes().iterator();
	}
}
