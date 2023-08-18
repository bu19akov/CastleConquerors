package messagesbase.messagesfromserver;

import java.util.Collection;
import java.util.Collections;
import java.util.HashSet;
import java.util.Iterator;
import java.util.Optional;
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

	@XmlElement(name = "maxX")
	private int maxX;
	@XmlElement(name = "maxY")
	private int maxY;

	public FullMap() {
		this.maxX = 0;
		this.maxY = 0;
	}

	public FullMap(Collection<FullMapNode> mapNodes) {
		if (mapNodes != null && !mapNodes.isEmpty()) {
			this.mapNodes.addAll(mapNodes);
		}
	}

	public Collection<FullMapNode> getMapNodes() {
		return Collections.unmodifiableCollection(mapNodes);
	}
	
	public Optional<FullMapNode> get(int x, int y) {
	    for (FullMapNode node : mapNodes) {
	        if (x == node.getX() && y == node.getY()) {
	            return Optional.of(node);
	        }
	    }
	    return Optional.empty();
	}
	
	public void add(FullMapNode node) {
		mapNodes.add(node);
	}
	
	public void remove(FullMapNode node) {
		mapNodes.remove(node);
	}
	
	public void removeAll() {
		this.mapNodes.clear();
	}
	
	public void addDefaultForTerrain(ETerrain terrain, int x, int y) {
		if (get(x, y).isPresent()) {
			remove(get(x,y).get());
		}
		mapNodes.add(new FullMapNode(terrain, EPlayerPositionState.NoPlayerPresent, ETreasureState.NoOrUnknownTreasureState, EFortState.NoOrUnknownFortState, x, y, 0));
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

	public int getMaxX() {
		return maxX;
	}

	public void setMaxX(int maxX) {
		this.maxX = maxX;
	}

	public int getMaxY() {
		return maxY;
	}

	public void setMaxY(int maxY) {
		this.maxY = maxY;
	}

	@Override
	public String toString() {
		return "FullMap [mapNodes=" + mapNodes + "]";
	}
}
