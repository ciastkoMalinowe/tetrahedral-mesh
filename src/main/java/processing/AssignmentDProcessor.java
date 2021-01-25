package processing;

import model.*;

import java.util.*;
import java.util.function.Predicate;
import java.util.stream.Collectors;

public class AssignmentDProcessor extends AssignmentAProcessor {

    @Override
    public String getProcessorId() { return "zadD"; }

    public static Comparator<GraphNode> byX = Comparator.comparing(
            x -> x.getCoordinates().getX()
    );

    public static Comparator<GraphNode> byY = Comparator.comparing(
            x -> x.getCoordinates().getY()
    );

    @Override
    protected TetrahedralGraph processGraphInternal(TetrahedralGraph graph) {

        graph = super.processGraphInternal(graph);

        List<InteriorNode> outerInteriorNodes = graph.getInteriorNodesByLevel(graph.getMaxLevel())
                .stream()
                .filter(node -> node.getSiblings().anyMatch(this::isOuter))
                .collect(Collectors.toList());

        for(InteriorNode node : outerInteriorNodes){
            applyProduction(10, graph, node, Collections.emptyList());
        }


        List<InteriorNode> innerInteriorNodes = graph.getInteriorNodesByLevel(graph.getMaxLevel() - 1)
                .stream()
                .filter(node -> node.getSiblings().allMatch(this::isInner))
                .collect(Collectors.toList());

        for(InteriorNode node : innerInteriorNodes){
            applyProduction(2, graph, node, Collections.emptyList());
        }

        double[] vars = {-0.5, 0, 0.5};
        for(double v : vars){
            apply13(graph, new Point2d(-1.0, v), new Point2d(-0.5,v));
            apply13(graph, new Point2d(0.5, v), new Point2d(1.0,v));
            apply13(graph, new Point2d(v,-1.0), new Point2d(v, -0.5));
            apply13(graph, new Point2d(v,0.5), new Point2d(v, 1.0));
        }

        apply7(graph, new Point2d(-0.25,0), new Point2d(-0.25,0.25), point -> point.getY() == 0);
        apply7(graph, new Point2d(0.25,0), new Point2d(0.25,0.25), point -> point.getY() == 0);
        apply7(graph, new Point2d(0,0.25), new Point2d(0.25,0.25), point -> point.getX() == 0);

        applyFake11(graph);

        apply12(graph, new Point2d(-0.5, -0.5), new Point2d(-0.5,-0.25), new Point2d(-0.5,0));
        apply12(graph, new Point2d(-0.5, 0), new Point2d(-0.5,0.25), new Point2d(-0.5,0.5));
        apply12(graph, new Point2d(-0.5, 0.5), new Point2d(-0.25,0.5), new Point2d(0,0.5));
        apply12(graph, new Point2d(0, 0.5), new Point2d(0.25,0.5), new Point2d(0.5,0.5));
        apply12(graph, new Point2d(0.5, 0.5), new Point2d(0.5,0.25), new Point2d(0.5,0));
        apply12(graph, new Point2d(0.5, 0), new Point2d(0.5,-0.25), new Point2d(0.5,-0.5));
        apply12(graph, new Point2d(0.5, -0.5), new Point2d(0.25,-0.5), new Point2d(0,-0.5));

        List<Point2d> points = Arrays.asList(new Point2d(0,0), new Point2d(0,-0.25), new Point2d(0,-0.5));

        List<GraphNode> nodes = graph.getGraphNodesByLevel(graph.getMaxLevel()).stream()
                .filter(node -> points.contains(node.getCoordinates()))
                .collect(Collectors.toList());

        applyProduction(9, graph, null, nodes);

        return graph;
    }

    private boolean isOuter(GraphNode n){
        return n.getCoordinates().getX() == 1 || n.getCoordinates().getY() == 1 ||
                n.getCoordinates().getX() == -1 || n.getCoordinates().getY() == -1;
    }

    private boolean isInner(GraphNode n){
        return ! isOuter(n);
    }

    private void apply7(TetrahedralGraph graph, Point2d point1, Point2d point2, Predicate<Point2d> onLine ){
        List<GraphNode> nodes = graph.getInteriorNodesByLevel(graph.getMaxLevel()).stream()
                .filter(node -> node.getSiblings().anyMatch(n -> n.getCoordinates().equals(point1)))
                .filter(node -> node.getSiblings().anyMatch(n -> n.getCoordinates().equals(point2)))
                .flatMap(NodeBase::getSiblings)
                .filter(node -> onLine.test(node.getCoordinates()))
                .distinct()
                .sorted(byX.thenComparing(byY))
                .collect(Collectors.toList());
        applyProduction(7, graph, null, nodes);
    }

    private void apply13(TetrahedralGraph graph, Point2d point1, Point2d point2){
        List<GraphNode> nodes = graph.getInteriorNodesByLevel(graph.getMaxLevel()).stream()
                .filter(node -> node.getSiblings().anyMatch(n -> n.getCoordinates().equals(point1)))
                .filter(node -> node.getSiblings().anyMatch(n -> n.getCoordinates().equals(point2)))
                .findFirst()
                .get()
                .getSiblings()
                .filter(n -> n.getCoordinates().equals(point1) || n.getCoordinates().equals(point2))
                .collect(Collectors.toList());

        applyProduction(13, graph, null, nodes);
    }

    private void applyFake11(TetrahedralGraph graph){
        List<GraphNode> nodes = graph.getGraphNodesByLevel(graph.getMaxLevel()).stream()
                .filter(node -> node.getCoordinates().getX() == 0 && node.getCoordinates().getY() == -0.5)
                .filter(node -> node.getSiblings().anyMatch(n -> n.getCoordinates().equals(new Point2d(-0.25, -0.5)) ||
                        n.getCoordinates().equals(new Point2d(-0.5, -0.5))))
                .collect(Collectors.toList());
        graph.mergeNodes(nodes.get(0), nodes.get(1));

        List<GraphNode> nodes2 = graph.getGraphNodesByLevel(graph.getMaxLevel()).stream()
                .filter(node -> node.getCoordinates().equals(new Point2d(-0.5, -0.5)) ||
                        node.getCoordinates().equals(new Point2d(-0.25, -0.5)))
                .sorted(byX.thenComparing(byY))
                .collect(Collectors.toList());

        List<GraphNode> result = graph.getGraphNodesByLevel(graph.getMaxLevel() - 1).stream()
                .filter(node -> node.getCoordinates().getX() == 0 && node.getCoordinates().getY() == -0.5)
                .collect(Collectors.toList());
        result.add(nodes.get(1));
        result.addAll(nodes2);

        applyProduction(12, graph, null, result);
    }

    private void apply12(TetrahedralGraph graph, Point2d point1, Point2d point2, Point2d point3) {
        List<GraphNode> nodes = graph.getGraphNodesByLevel(graph.getMaxLevel()).stream()
                .filter(node -> node.getCoordinates().equals(point1) ||
                        node.getCoordinates().equals(point2) ||
                        node.getCoordinates().equals(point3))
                .collect(Collectors.toList());

        List<GraphNode> result = graph.getGraphNodesByLevel(graph.getMaxLevel() - 1).stream()
                .filter(node -> node.getCoordinates().equals(point1))
                .collect(Collectors.toList());
        result.addAll(nodes);

        applyProduction(12, graph, null, result);
    }
}
