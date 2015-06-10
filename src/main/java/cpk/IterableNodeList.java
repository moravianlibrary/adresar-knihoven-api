package cpk;

import java.util.Iterator;
import java.util.NoSuchElementException;

import org.w3c.dom.Node;
import org.w3c.dom.NodeList;

/**
 * Simple org.w3c.dom.NodeList iterable wrapper that can be used with Java's
 * foreach loop.
 *
 * @author Trevor Pounds
 */
public class IterableNodeList implements Iterable<Node>, Iterator<Node> {
    /** List of nodes */
    private final NodeList nodeList;

    /** List of nodes current index. */
    private int index = 0;

    /** Constructs iterable node list. */
    public IterableNodeList(final NodeList nodeList) {
        this.nodeList = nodeList;
    }

    /** {@inheritDoc} */
    public boolean hasNext() {
        return index < nodeList.getLength();
    }

    /** {@inheritDoc} */
    public Iterator<Node> iterator() {
        return this;
    }

    /** {@inheritDoc} */
    public Node next() throws NoSuchElementException {
        if (!hasNext()) {
            throw new NoSuchElementException();
        }
        return nodeList.item(index++);
    }

    /** {@inheritDoc} */
    public void remove() throws IllegalStateException, UnsupportedOperationException {
        throw new UnsupportedOperationException();
    }
}