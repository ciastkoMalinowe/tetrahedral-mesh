package processing;

import common.CustomCollectors;
import model.*;

import java.util.*;
import java.util.function.Function;
import java.util.stream.Collectors;
import java.util.stream.IntStream;
import java.util.stream.Stream;

public class AssignmentCProcessor extends AbstractProcessor {

    @Override
    public String getProcessorId() {
        return "zadC";
    }

    public static Comparator<GraphNode> byX = Comparator.comparing(
            x -> x.getCoordinates().getX()
    );

    public static Comparator<GraphNode> byY = Comparator.comparing(
            x -> x.getCoordinates().getY()
    );

    public static Comparator<Set<GraphNode>> squareByX = Comparator.comparing(
            x -> x.stream().map(GraphNode::getCoordinates).map(Point2d::getX).min(Double::compare).orElse(0.0)
    );

    public static Comparator<Set<GraphNode>> squareByY = Comparator.comparing(
            x -> x.stream().map(GraphNode::getCoordinates).map(Point2d::getY).min(Double::compare).orElse(0.0)
    );

    public static Stream<GraphNode> bottomNodes(Set<GraphNode> square) {
        double maxY = square.stream().map(GraphNode::getCoordinates).map(Point2d::getY).min(Double::compare).orElse(0.0);
        return square.stream().filter(node -> node.getCoordinates().getY() == maxY);
    }

    public static Stream<GraphNode> topNodes(Set<GraphNode> square) {
        double maxY = square.stream().map(GraphNode::getCoordinates).map(Point2d::getY).max(Double::compare).orElse(0.0);
        return square.stream().filter(node -> node.getCoordinates().getY() == maxY);
    }

    public void mergeHorizontalWith(TetrahedralGraph graph, int prodNumber, Set<InteriorNode> interiorNodes) {
        List<Set<GraphNode>> squares = interiorNodes
                .stream()
                .map(InteriorNode::getChildren)
                .map(
                        nodes -> nodes.map(
                                node -> node.getSiblings().collect(Collectors.toList())
                        ).flatMap(Collection::stream).collect(Collectors.toSet())
                )
                .sorted(squareByX.thenComparing(squareByY))
                .collect(Collectors.toList());

        for (int i = 0; i < squares.size() - 1; i++) {
            List<GraphNode> topEdge = topNodes(squares.get(i)).sorted(byX.thenComparing(byY)).collect(Collectors.toList());
            List<GraphNode> bottomEdge = bottomNodes(squares.get(i + 1)).sorted(byX.thenComparing(byY.reversed())).collect(Collectors.toList());

            boolean nodesAreMatching = topEdge.size() == bottomEdge.size() && IntStream.range(0, topEdge.size()).allMatch(idx ->
                    topEdge.get(idx).getCoordinates().equals(bottomEdge.get(idx).getCoordinates())
            );
            boolean prodApplies = (prodNumber == 13 && topEdge.size() == 2) || (prodNumber == 7 && topEdge.size() == 3);

            if (nodesAreMatching && prodApplies) {
                applyProduction(prodNumber, graph, null, topEdge);
            }
        }
    }


    public void breakLowestLevel(TetrahedralGraph graph) {
        int currentLevel = graph.getMaxLevel();

        double maxX = graph.getGraphNodesByLevel(currentLevel)
                .stream()
                .map(x -> x.getCoordinates().getX())
                .max(Double::compare).orElse(0.0);

        List<GraphNode> rightmostGraphNodes = graph.getGraphNodesByLevel(currentLevel)
                .stream()
                .filter(x -> x.getCoordinates().getX() == maxX)
                .collect(Collectors.toList());

        Stream<Stream<InteriorNode>> rightmostInteriorsStreams = rightmostGraphNodes
                .stream()
                .map(GraphNode::getInteriors);

        Set<InteriorNode> rightmostInteriors = rightmostInteriorsStreams
                .flatMap(Function.identity())
                .collect(Collectors.toSet());


        rightmostInteriors.forEach(interior -> applyProduction(2, graph, interior, Collections.emptyList()));

        Set<InteriorNode> leftInteriors = graph.getInteriorNodesByLevel(currentLevel)
                .stream()
                .filter(interior -> !rightmostInteriors.contains(interior))
                .collect(Collectors.toSet());

        for (InteriorNode interior : leftInteriors) {
            applyProduction(10, graph, interior, Collections.emptyList());
        }

        mergeHorizontalWith(graph, 13, leftInteriors);
        mergeHorizontalWith(graph, 7, rightmostInteriors);
    }


    @Override
    protected TetrahedralGraph processGraphInternal(TetrahedralGraph graph) {
        InteriorNode entryNode = graph
                .getInteriorNodes()
                .stream()
                .filter(x -> x.getSymbol().equals("E"))
                .collect(CustomCollectors.toSingle());

        applyProduction(1, graph, entryNode, Collections.emptyList());

        InteriorNode level1Interior = entryNode.getChildren().collect(CustomCollectors.toSingle());

        applyProduction(2, graph, level1Interior, Collections.emptyList());

        breakLowestLevel(graph);
        breakLowestLevel(graph);

        return graph;
    }
}
