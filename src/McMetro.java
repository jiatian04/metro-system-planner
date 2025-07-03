import java.util.*;
import java.lang.Math.*;

public class McMetro {
    protected Track[] tracks;
    protected HashMap<BuildingID, Building> buildingTable = new HashMap<>();
    private HashMap<Building, HashMap<Building, Integer>> adjacencyList = new HashMap<>();
    private TrieNode root;

    // Constructor method
    McMetro(Track[] tracks, Building[] buildings) {
        // Initialize tracks
        this.tracks = tracks;
        if (tracks == null) {
           this.tracks = new Track[0];
        }

       List<Track> validTracks = new ArrayList<>();
       for (Track track : this.tracks) {
           if (track != null && track.startBuildingId() != null && track.endBuildingId() != null) {
               validTracks.add(track);
           }
       }
       this.tracks = validTracks.toArray(new Track[0]);

       // Initialize the Trie root node
       this.root = new TrieNode();

       if (buildings == null) {
           buildings = new Building[0];
       }
       // Populate buildings table
       for (Building building : buildings) {
           buildingTable.putIfAbsent(building.id(), building);
       }

       // Build adjacency list
       for (Track track : this.tracks) {
           BuildingID startBuildingId = track.startBuildingId();
           Building startBuilding = buildingTable.get(startBuildingId);
           BuildingID endBuildingId = track.endBuildingId();
           Building endBuilding = buildingTable.get(endBuildingId);
           int trackCapacity = track.capacity();

           int actualCapacity = Math.min(startBuilding.occupants(), endBuilding.occupants());
           actualCapacity = Math.min(actualCapacity, trackCapacity);

           adjacencyList.putIfAbsent(startBuilding, new HashMap<>());
           adjacencyList.get(startBuilding).put(endBuilding, actualCapacity);
       }
    }

    // Maximum number of passengers that can be transported from start to end
    int maxPassengers(BuildingID start, BuildingID end) {
        Building source = buildingTable.get(start);
        Building sink = buildingTable.get(end);

        if (source == null || sink == null) {
            return 0;
        }

        if (source.equals(sink)) {
            return 0;
        }

        return maxFlow(source, sink);
    }

    // reference: https://www.geeksforgeeks.org/ford-fulkerson-algorithm-for-maximum-flow-problem/
    private boolean bfs(Building source, Building sink, Map<Building, Building> parentMap) {
        Queue<Building> queue = new LinkedList<>();
        Set<Building> visited = new HashSet<>();
        queue.add(source);
        visited.add(source);

        while(!queue.isEmpty()) {
            Building current = queue.poll();
            for (Map.Entry<Building, Integer> entry : adjacencyList.getOrDefault(current, new HashMap<>()).entrySet()) {
                Building next = entry.getKey();
                int capacity = entry.getValue();
                if (!visited.contains(next) && capacity > 0) {
                    parentMap.put(next, current);
                    if (next.equals(sink)) {
                        return true;
                    }
                    queue.add(next);
                    visited.add(next);
                }
            }
        }
        return false;
    }

    // reference: https://www.geeksforgeeks.org/ford-fulkerson-algorithm-for-maximum-flow-problem/
    private int maxFlow(Building source, Building sink) {
        Map<Building, Building> parentMap = new HashMap<>();
        int maxFlow = 0;

        while (bfs(source, sink, parentMap)) {
            int pathFlow = Integer.MAX_VALUE;
            for (Building v = sink; !v.equals(source); v = parentMap.get(v)) {
                Building u = parentMap.get(v);
                pathFlow = Math.min(pathFlow, adjacencyList.get(u).get(v));
            }

            for (Building v = sink; !v.equals(source); v = parentMap.get(v)) {
                Building u = parentMap.get(v);
                adjacencyList.get(u).put(v, adjacencyList.get(u).get(v) - pathFlow);
            }

            maxFlow += pathFlow;
        }

        // Reset capacities to their original values for reuse
        for (Track track : this.tracks) {
            BuildingID startBuildingId = track.startBuildingId();
            Building startBuilding = buildingTable.get(startBuildingId);
            BuildingID endBuildingId = track.endBuildingId();
            Building endBuilding = buildingTable.get(endBuildingId);
            int trackCapacity = track.capacity();

            int actualCapacity = Math.min(startBuilding.occupants(), endBuilding.occupants());
            actualCapacity = Math.min(actualCapacity, trackCapacity);

            adjacencyList.get(startBuilding).put(endBuilding, actualCapacity);
        }
        return maxFlow;
    }

    // Returns a list of trackIDs that connect to every building maximizing total network capacity taking cost into account
    // reference: https://www.geeksforgeeks.org/minimum-spanning-tree-using-priority-queue-and-array-list/
    TrackID[] bestMetroSystem() {
        if (this.tracks.length == 0) {
            return new TrackID[0];
        }

        PriorityQueue<Track> trackQueue = new PriorityQueue<>((a, b) -> {
            int goodnessA = goodness(a);
            int goodnessB = goodness(b);
            return Integer.compare(goodnessB, goodnessA);
        });

        trackQueue.addAll(Arrays.asList(this.tracks));

        NaiveDisjointSet<Building> nds = new NaiveDisjointSet<>();
        for (Building building : buildingTable.values()) {
            nds.add(building);
        }

        TrackID[] mst = new TrackID[buildingTable.size() - 1];
        int index = 0;

        while (!trackQueue.isEmpty() && index < mst.length) {
            Track track = trackQueue.poll();
            Building startBuilding = buildingTable.get(track.startBuildingId());
            Building endBuilding = buildingTable.get(track.endBuildingId());
            if (nds.find(startBuilding).compareTo(nds.find(endBuilding)) != 0) {
                nds.union(startBuilding, endBuilding);
                mst[index] = track.id();
                index++;
            }
        }
        return mst;
    }

    private int goodness(Track track) {
        BuildingID startBuildingId = track.startBuildingId();
        Building startBuilding = buildingTable.get(startBuildingId);
        BuildingID endBuildingId = track.endBuildingId();
        Building endBuilding = buildingTable.get(endBuildingId);
        int trackCapacity = track.capacity();

        int actualCapacity = Math.min(startBuilding.occupants(), endBuilding.occupants());
        actualCapacity = Math.min(actualCapacity, trackCapacity);

        return actualCapacity / track.cost();
    }

    // reference: https://www.baeldung.com/trie-java
    private class TrieNode {
        Map<Character, TrieNode> children = new HashMap<>();
        Set<String> names = new HashSet<>();
    }

    // Adds a passenger to the system
    // reference: https://www.baeldung.com/trie-java
    void addPassenger(String name) {
        name = name.substring(0, 1).toUpperCase() + name.substring(1).toLowerCase();
        TrieNode current = root;

        for (char c : name.toCharArray()) {
            current = current.children.computeIfAbsent(c, k -> new TrieNode());
            current.names.add(name);
        }
    }

    void addPassengers(String[] names) {
        for (String s : names) {
            addPassenger(s);
        }
    }

    // Returns all passengers in the system whose names start with firstLetters
    // reference: https://www.baeldung.com/trie-java
    ArrayList<String> searchForPassengers(String firstLetters) {
        if (firstLetters == null) {
            return new ArrayList<>();
        }

        firstLetters = firstLetters.substring(0, 1).toUpperCase() + firstLetters.substring(1).toLowerCase();
        TrieNode current = root;

        for (char c : firstLetters.toCharArray()) {
            if (!current.children.containsKey(c)) {
                return new ArrayList<>();
            }
            current = current.children.get(c);
        }

        ArrayList<String> result = new ArrayList<>(current.names);
        Collections.sort(result);

        return result;
    }

    // Return how many ticket checkers will be hired
    // reference: https://www.geeksforgeeks.org/activity-selection-problem-greedy-algo-1/
    static int hireTicketCheckers(int[][] schedule) {
        Arrays.sort(schedule, Comparator.comparingInt(a -> a[1]));

        int i, j, count;
        i = 0;
        count = 0;

        if (schedule.length != 0) {
            count++;
        }

        for (j = 1; j < schedule.length; j++) {
            if (schedule[j][0] >= schedule[i][1]) {
                count++;
                i = j;
            }
        }
        return count;
    }
}
