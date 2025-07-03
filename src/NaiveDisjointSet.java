import java.util.*;

public class NaiveDisjointSet<T> {
    HashMap<T, T> parentMap = new HashMap<>();
    HashMap<T, Integer> rankMap = new HashMap<>();

    void add(T element) {
        parentMap.put(element, element);
        rankMap.put(element, 1);
    }

    // Path compression
    // reference: https://www.geeksforgeeks.org/union-by-rank-and-path-compression-in-union-find-algorithm/
    T find(T a) {
        T node = parentMap.get(a);
        if (node.equals(a)) {
            return node;
        } else {
            node = find(parentMap.get(a));
            return node;
        }
    }

    // Union by rank
    // reference: https://www.geeksforgeeks.org/union-by-rank-and-path-compression-in-union-find-algorithm/
    void union(T a, T b) {
        T rootA = find(a);
        T rootB = find(b);

        if (!rootA.equals(rootB)) {
            int rankA = rankMap.get(rootA);
            int rankB = rankMap.get(rootB);

            if (rankA > rankB) {
                parentMap.put(rootB, rootA);
            } else if (rankB > rankA) {
                parentMap.put(rootA, rootB);
            } else {
                parentMap.put(rootB, rootA);
                rankMap.put(rootA, rankA + 1);
            }
        }
    }
}
