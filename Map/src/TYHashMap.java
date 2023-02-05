import java.util.Comparator;
import java.util.LinkedList;
import java.util.Objects;
import java.util.Queue;

public class TYHashMap<K, V> implements TYMap<K, V> {


    private Comparator<K> comparator;   // 比较器

    public TYHashMap(Comparator<K> comparator) {
        this.comparator = comparator;
    }

    private static final boolean RED = false;
    private static final boolean BLACK = true;
    private int size;

    // 哈希表数组, 这里用红黑树实现, 不用链表. 数组只存放红黑树根节点, 而不是存放整颗红黑树
    // 当哈希冲突时, 一个数组索引对应的就是一颗红黑树
    private Node<K,V>[] table;

    // 为了提高效率, 使用 & 位运算取代 % 运算, 前提是将数组长度设计为 2 的幂次方
    private static final int DEFAULT_CAPACITY = 1 << 4;

    public TYHashMap() {
        // 默认给数组各最大容量
        table = new Node[DEFAULT_CAPACITY];
        this.comparator = null;
    }

    @Override
    public int size() {
        return size;
    }

    @Override
    public boolean isEmpty() {
        return size == 0;
    }

    @Override
    public void clear() {
        if (size == 0) return;
        size = 0;
        // 清空数组元素
        for (int i = 0; i < table.length; i++) {
            table[i] = null;
        }
    }

    @Override
    public V put(K key, V value) {

        // 取出元素索引
        int index = index(key);
        // 取出 index 位置的红黑树根节点
        Node<K,V> root = table[index];
        // 处理根节点为空的情况
        if (root == null) {
            // 创建根结点
            root = new Node<>(key, value, null);
            // 放入数组中
            table[index] = root;
            size++;
            // 红黑树添加后平衡处理
            afterPut(root);
            return null;
        }

        // 根结点不为空, 说明已经有元素了, 处理哈希冲突, 添加新的节点到红黑树上
        Node<K,V> parent = root;
        Node<K,V> node = root;
        int cmp = 0;
        K k1 = key;
        int h1 = k1 == null ? 0 : k1.hashCode();
        Node<K,V> result = null;
        boolean searched = false;
        do {
//            cmp = compare(key, node.key);
//            cmp = compare(key, node.key, keyHashCode, node.hashCode);
            parent = node;
            K k2 = node.key;
            int h2 = node.hashCode;
            if (h1 > h2) {
                // 右
                cmp = 1;
            } else if (h1 < h2) {
                // 左
                cmp = -1;
            } else if (Objects.equals(k1, k2)) {
                cmp = 0;
            } else if (k1 != null && k2 != null
                    && k1.getClass() == k2.getClass()
                    && k1 instanceof Comparable
                    && (cmp = ((Comparable) k1).compareTo(k2)) != 0) {

            } else if (searched) {
                // searched == true 的情况
                cmp = System.identityHashCode(k1) - System.identityHashCode(k2);
            } else {
                // searched == false 的情况, 然后再根据内存地址大小决定左右
                if ((node.left != null && (result = node(node.left, k1)) != null) || (node.right != null && (result = node(node.right, k1)) != null)) {
                    // 已经存在这个 key
                    node = result;
                    cmp = 0;
                } else {    // 不存在这个 key
                    searched = true;
                    // 内存地址比大小
                    cmp = System.identityHashCode(k1) - System.identityHashCode(k2);
                }
            }

            if (cmp > 0) {
                node = node.right;
            } else if (cmp < 0) {
                node = node.left;
            } else {
                V oldValue = node.value;
                node.key = key;
                node.value = value;
                return oldValue;
            }

        } while (node != null);

        Node<K,V> newNode = new Node<>(key,value,parent);
        if (cmp > 0) {
            parent.right = newNode;
        } else {
            parent.left = newNode;
        }
        size++;

        afterPut(newNode);

        return null;
    }

    private void afterPut(Node<K,V> node) {
        // 父节点
        Node<K,V> parent = node.parent;

        // 1. 如果添加的是根节点, 直接染成黑色
        if (parent == null) {
            black(node);
            return;
        }

        // 2. 第一种四种情况中, 父节点是黑色的情况, 不用处理
        if (isBLACK(parent)) return;

        // 3. 其余 8 种父节点是红色的情况
        // 找到 uncle 节点
        Node<K,V> uncle = parent.sibling();
        // 祖父节点
        Node<K,V> grand = parent.parent;
        // 3.1 叔父节点是红色的情况
        if (isRED(uncle)) {
            // 父节点染成黑色
            black(parent);
            // 叔父节点染成黑色
            black(uncle);
            // 祖父节点当做新添加的节点, 染成红色, 递归
            afterPut(red(grand));
            return;
        }

        // 4. 叔父节点不是红色的情况

        // 父节点是左子树情况
        if (parent.isLeftChild()) { // L
            // 新添加节点是左子树情况
            if (node.isLeftChild()) {
                // LL
                black(parent);
                red(grand);
                rotateRight(grand);
            } else {
                // LR
                black(node);
                red(grand);
                rotateLeft(parent);
                rotateRight(grand);
            }

        } else {    // R

            if (node.isLeftChild()) {
                // RL
                black(node);
                red(grand);
                rotateRight(parent);
                rotateLeft(grand);
            } else {
                // RR
                black(parent);
                red(grand);
                rotateLeft(grand);
            }
        }
    }

    @Override
    public V get(K key) {
        Node<K,V> node = node(key);
        return node == null ? null : node.value;
    }

    @Override
    public V remove(K key) {
        return remove(node(key));
    }

    private V remove(Node<K,V> node) {
        if (node == null) return null;
        size--;

        V oldValue = node.value;

        // 度为 2 的节点
        if (node.hasTwoChildren()) {
            // 找后继节点
            Node<K,V> s = successor(node);
            node.key = s.key;
            node.value = s.value;
            node = s;
        }

        // 删除 node 节点, (node 的度必然是 1 或者 0)
        Node<K,V> replacement = node.left != null ? node.left : node.right;

        // 找到红黑树所在数组索引
        int index = index(node);

        // 度为 1 的节点
        if (replacement != null) {
            // 更改 parent
            replacement.parent = node.parent;
            // 更改 parent 的 left, right 指向

            // node 是度为 1 的节点,且是根节点
            if (node.parent == null) {
                table[index] = replacement;
            } else if (node == node.parent.left) {
                node.parent.left = replacement;
            } else {
                node.parent.right = replacement;
            }

            // 删除节点之后处理
            afterRemove(node, replacement);

        } else if (node.parent == null) { // node 是叶子节点, 并且是根节点
            table[index] = null;
        } else {    // node 是叶子节点,但不是根节点
            if (node == node.parent.left) {
                node.parent.left = null;
            } else if (node == node.parent.right) {
                node.parent.right = null;
            }

            afterRemove(node, null);
        }

        return oldValue;

    }

    protected void afterRemove(Node<K,V> node, Node<K,V> replacement) {

        // 1. 如果删除的节点是红色, 不处理
        if (isRED(node)) return;

        // 2. 删除节点为黑色
        // 删除节点的子节点是红色
        if (isRED(replacement)) {
            // 染成黑色, 删除度为 1 的操作, 父类已经处理完成
            black(replacement);
            return;
        }

        Node<K,V> parent = node.parent;
        // 如果删除的节点 BLACK 是根节点, 不处理
        if (parent == null) return;

        // 删除的是黑色叶子节点
        // 判断被删除的节点 node 是左还是右
        boolean left = parent.left == null;
        // 找到其兄弟节点, 不能用 node.sibling(), 因为在此刻, 那个方法求出的兄弟节点不准确了
        Node<K,V> sibling = left ? parent.right : parent.left;
        if (left) {
            // 被删除的节点在左, 兄弟节点在右, 逻辑与下面对称

            // 如果兄弟节点是 RED 的情况
            if (isRED(sibling)) {
                black(sibling);
                red(parent);
                rotateLeft(parent);

                // 更换兄弟节点
                sibling = parent.right;
            }

            // 走到这里, 兄弟节点必然是 BLACK
            if (isBLACK(sibling.left) && isBLACK(sibling.right)) {
                // 兄弟节点都是 BLACK, 父节点要向下和兄弟节点合并

                boolean parentBlack = isBLACK(parent);
                black(parent);
                red(sibling);
                // 判断父节点是 BLACK 的情况
                if (parentBlack) {
                    afterRemove(parent, null);
                }

            } else {
                // 兄弟节点至少有一个 RED 子节点, 向兄弟节点借元素

                // 先处理左边是黑色的情况, 先左旋兄弟节点, 后面情况基本一致
                if (isBLACK(sibling.right)) {
                    rotateRight(sibling);
                    sibling = parent.right;
                }
                color(sibling, colorOf(parent));
                black(sibling.right);
                black(parent);
                rotateLeft(parent);
            }

        } else {
            // 被删除的节点在右, 兄弟节点在左

            // 如果兄弟节点是 RED 的情况
            if (isRED(sibling)) {
                black(sibling);
                red(parent);
                rotateRight(parent);

                // 更换兄弟节点
                sibling = parent.left;
            }

            // 走到这里, 兄弟节点必然是 BLACK
            if (isBLACK(sibling.left) && isBLACK(sibling.right)) {
                // 兄弟节点都是 BLACK, 父节点要向下和兄弟节点合并

                boolean parentBlack = isBLACK(parent);
                black(parent);
                red(sibling);
                // 判断父节点是 BLACK 的情况
                if (parentBlack) {
                    afterRemove(parent, null);
                }

            } else {
                // 兄弟节点至少有一个 RED 子节点, 向兄弟节点借元素

                // 先处理左边是黑色的情况, 先左旋兄弟节点, 后面情况基本一致
                if (isBLACK(sibling.left)) {
                    rotateLeft(sibling);
                    sibling = parent.left;
                }
                color(sibling, colorOf(parent));
                black(sibling.left);
                black(parent);
                rotateRight(parent);
            }

        }

    }

    @Override
    public boolean containsKey(K key) {
        return node(key) != null;
    }

    @Override
    public boolean containsValue(V value) {

        if (size == 0) return false;

        // 使用层序遍历, 遍历数组中每个红黑树的所有节点, 看是否 value 相同
        Queue<Node<K,V>> queue = new LinkedList<>();

        for (int i = 0; i < table.length; i++) {

            if (table[i] == null) continue;

            queue.offer(table[i]);

            while (!queue.isEmpty()) {
                Node<K,V> node = queue.poll();
                if (Objects.equals(value, node.value)) return true;

                if (node.left != null) {
                    queue.offer(node.left);
                }

                if (node.right != null) {
                    queue.offer(node.right);
                }
            }
        }

        return false;
    }

    @Override
    public void traversal(Visitor<K, V> visitor) {

        if (size == 0) return;

        Queue<Node<K,V>> queue = new LinkedList<>();
        for (int i = 0; i < table.length; i++) {
            if (table[i] == null) continue;

            while (!queue.isEmpty()) {

                Node<K,V> node = queue.poll();
                if (visitor.visit(node.key, node.value)) return;

                if (node.left != null) {
                    queue.offer(node.left);
                }
                if (node.right != null) {
                    queue.offer(node.right);
                }
            }

        }

    }

    /**
     * 根据 Key 生成对应的索引 (在桶数组中的位置)
     * @param key key
     * @return 索引
     */
    private int index(K key) {
        if (key == null) return 0;
        int hash = key.hashCode();
        hash = hash ^ (hash >>> 16);    // hash >>> 16. 无符号右移 16 位. 与 hash 做异或 (^), 增强hashCode
        return hash & (table.length - 1);
    }

    private int index(Node<K,V> node) {
        return (node.hashCode ^ (node.hashCode >>> 16)) & (table.length - 1);
    }

    private Node<K,V> node(K key) {

        Node<K,V> root = table[index(key)];
        return root == null ? null : node(root, key);

//        int index = index(key);
//        Node<K,V> node = table[index];
//        int key1HashCode = key == null ? 0 : key.hashCode();
//        while (node != null) {
//            int cmp = compare(key, node.key, key1HashCode, node.hashCode);
//            if (cmp == 0) return node;
//            if (cmp > 0) {
//                node = node.right;
//            }
//            if (cmp < 0) {
//                node = node.left;
//            }
//        }
//        return null;
    }

    private Node<K,V> node(Node<K,V> node, K k1) {
        int h1 = k1 == null ? 0 : k1.hashCode();
        // 存查找结果
        Node<K,V> result = null;
        int cmp = 0;
        while (node != null) {
            K k2 = node.key;
            int h2 = node.hashCode;
            // 先比较哈希值
            if (h1 > h2) {
                node = node.right;
            } else if (h1 < h2) {
                node = node.left;
            } else if (Objects.equals(k1, k2)) {
                return node;
            } else if (k1 != null && k2 != null
                        && k1.getClass() == k2.getClass()
                        && k1 instanceof Comparable
                        && (cmp = ((Comparable) k1).compareTo(k2)) != 0) {
                node = cmp > 0 ? node.right : node.left;
            } else if (node.right != null && (result = node(node.right, k1)) != null) {
                // 往右找
                return result;
            } else if (node.left != null && (result = node(node.left, k1)) != null) {
                // 往左找
                return result;
            } else {
                return null;
            }
        }
        return null;
    }


    private static class Node<K,V> {
        int hashCode;   // 防止后面用到时重复计算. 搞个属性存一下
        K key;
        V value;
        boolean color = RED;
        Node<K,V> left;
        Node<K,V> right;
        Node<K,V> parent;
        public Node(K key, V value, Node<K,V> parent) {
            this.key = key;
            this.hashCode = key == null ? 0 : key.hashCode();
            this.value = value;
            this.parent = parent;
        }

        public boolean isLeaf() {
            return left == null && right == null;
        }

        public boolean hasTwoChildren() {
            return left != null && right != null;
        }

        // 左子树
        public boolean isLeftChild() {
            return parent != null && this == parent.left;
        }

        // 右子树
        public boolean isRightChild() {
            return parent != null && this == parent.right;
        }

        // 兄弟节点
        public Node<K,V> sibling() {
            if (isLeftChild()) {
                return parent.right;
            }
            if (isRightChild()) {
                return parent.left;
            }
            return null;
        }

    }

    /**
     * 给节点染色
     * @param node 待染色节点
     * @param color 颜色, RED 或者 BLACK
     * @return 被染过色的节点
     */
    private Node<K,V> color(Node<K,V> node, boolean color) {
        if (node == null) return node;
        node.color = color;
        return node;
    }

    /**
     * 染红色
     * @param node 待染色节点
     * @return 红色节点
     */
    private Node<K,V> red(Node<K,V> node) {
        return color(node, RED);
    }

    /**
     * 染黑色
     * @param node 待染色节点
     * @return 黑色节点
     */
    private Node<K,V> black(Node<K,V> node) {
        return color(node, BLACK);
    }

    private boolean colorOf(Node<K,V> node) {
        return node == null ? BLACK : node.color;
    }

    private boolean isBLACK(Node<K,V> node) {
        return colorOf(node) == BLACK;
    }

    private boolean isRED(Node<K,V> node) {
        return colorOf(node) == RED;
    }

    /**
     * 左旋转
     * @param grand 节点
     */
    protected void rotateLeft(Node<K,V> grand) {
        // 找到 parent 节点, 能来到这, 说明 parent 是 grand 的左子树
        Node<K,V> parent = grand.left;
        Node<K,V> child = parent.left;

        // 旋转
        grand.right = child;
        parent.left = grand;

        afterRotate(grand, parent, child);

    }

    /**
     * 右旋转
     * @param grand 节点
     */
    protected void rotateRight(Node<K,V> grand) {

        Node<K,V> parent = grand.left;
        Node<K,V> child = parent.right;

        grand.left = child;
        parent.right = grand;

        afterRotate(grand, parent, child);

    }

    protected void afterRotate(Node<K,V> grand, Node<K,V> parent, Node<K,V> child) {
        // 让 parent 成为子树的根节点
        parent.parent = grand.parent;
        if (grand.isLeftChild()) {
            // grand 是其父节点的左子树
            grand.parent.left = parent;
        } else if (grand.isRightChild()) {
            // grand 是其父节点的右子树
            grand.parent.right = parent;
        } else {
            // grand 是根节点
            // root = table[index(grand.key)] 取出 root
            table[index(grand)] = parent;
        }

        // 更新 child 的 parent
        if (child != null) {
            child.parent = grand;
        }

        // 更新 grand 的 parent
        grand.parent = parent;
    }

    public void keyNotNullCheck(K key) {
        if (key == null) {
            throw new IllegalArgumentException("key 不能为空!");
        }
    }

    private int compare(K key1, K key2, int hashCode1, int hashCode2) {

        // 比较哈希值
        int result = hashCode1 - hashCode2;
        // 如果相减不为 0, 那么就返回结果, 看大小
        if (result != 0) return result;

        // 比较相等的情况处理, 如果哈希值相等, 不能说明key相同, 要看 equals

        // 如果 equals 也相同, 那么说明 key 相同, 返回 0
        if (Objects.equals(key1, key2)) return 0;

        // 来到这里, 说明哈希值相等, 但是 equals 不同

        // 比较类名
        if (key1 != null && key2 != null) {
            String key1Cls = key1.getClass().getName();
            String key2Cls = key2.getClass().getName();
            result = key1Cls.compareTo(key2Cls);
            // 如果结果不为 0, 说明不同, 返回结果大小
            if (result != 0) return result;

            // 同一种类型,且具备可比较性
            if (key1 instanceof Comparable) {
                return ((Comparable) key1).compareTo(key2);
            }
        }

        // 来到这里, 说明:
        // 同一种类型, 但是不具备可比较性
        // 或者 key1不为 null, key2 为 null
        // 或者 key1 为 null, key2 不为 null
        // 那么就比较内存地址
        return System.identityHashCode(key1) - System.identityHashCode(key2);

    }

    // 后继节点: [中序遍历]时,当前节点的后一个节点
    // 和找前驱相反
    protected Node<K,V> successor(Node<K,V> node) {
        if (node == null) return node;
        Node<K,V> s = node.right;
        if (s != null) {
            while (s.left != null) {
                s = s.left;
            }
            return s;
        }

        while (node.parent != null && node == node.parent.right) {
            node = node.parent;
        }
        return node.parent;
    }

}
