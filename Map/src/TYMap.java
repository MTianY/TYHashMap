public interface TYMap<K, V> {

    int size();
    boolean isEmpty();
    void clear();
    V put(K key, V value);
    V get(K key);
    V remove(K key);
    boolean containsKey(K key);
    boolean containsValue(V value);
    void traversal(Visitor<K,V> visitor);

    public static abstract class Visitor<K,V> {
        boolean stop;
        abstract boolean visit(K key, V value);
    }

}
