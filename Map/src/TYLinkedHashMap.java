
public class TYLinkedHashMap<K,V> extends TYHashMap<K,V> {

    private LinkedNode<K,V> first;
    private LinkedNode<K,V> last;

    @Override
    protected TYHashMap.Node<K, V> createNode(K key, V value, TYHashMap.Node<K, V> parent) {
        LinkedNode<K,V> node = new LinkedNode<>(key, value, parent);

        if (first == null) {
            first = last = node;
        } else {
            last.next = node;
            node.prev = last;
            last = node;
        }

        return node;
    }

    private static class LinkedNode<K,V> extends Node<K,V> {

        LinkedNode<K,V> prev;
        LinkedNode<K,V> next;

        public LinkedNode(K key, V value, Node<K,V> parent) {
            super(key, value, parent);
        }
    }

    @Override
    public void clear() {
        super.clear();
        first = null;
        last = null;
    }

    // 按链表方式来重新遍历
    @Override
    public void traversal(Visitor<K, V> visitor) {
        if (visitor == null) return;
        LinkedNode<K,V> node = first;
        while (node != null) {
            if (visitor.visit(node.key, node.value)) return;
            node = node.next;
        }
    }

    @Override
    protected void subAfterRemove(Node<K, V> twoChildrenNode, Node<K, V> removeNode) {
        LinkedNode<K,V> willNode = (LinkedNode<K, V>) twoChildrenNode;
        LinkedNode<K,V> linkedNode = (LinkedNode<K, V>) removeNode;

        if (willNode != linkedNode) {
            // 交换 willNode 和 removeNode 在链表中的位置
            // 交换 prev
            LinkedNode<K,V> tmp = willNode.prev;
            willNode.prev = linkedNode.prev;
            linkedNode.prev = tmp;

            if (willNode.prev == null) {
                first = willNode;
            } else {
                willNode.prev.next = willNode;
            }
            if (linkedNode.prev == null) {
                first = linkedNode;
            } else {
                linkedNode.prev.next = linkedNode;
            }

            // 交换 next
            tmp = willNode.next;
            willNode.next = linkedNode.next;
            linkedNode.next = tmp;
            if (willNode.next == null) {
                last = willNode;
            } else {
                willNode.next.prev = willNode;
            }
            if (linkedNode.next == null) {
                last = linkedNode;
            } else {
                linkedNode.next.prev = linkedNode;
            }

        }

        LinkedNode<K,V> prev = linkedNode.prev;
        LinkedNode<K,V> next = linkedNode.next;
        if (prev == null) {
            first = next;
        } else {
            prev.next = next;
        }

        if (next == null) {
            last = prev;
        } else {
            next.prev = prev;
        }

    }
}
