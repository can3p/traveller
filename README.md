# How would you traverse a graph?

If you can traverse, everything looks like a graph. How do you do it?
If the only purpose is to visit all nodes, it actually doesn't matter
whether it will be a depth first or breadth first, because, well, we
need to visit them all.

Let's do some pseudo code.

``` javascript
function traverse(node) {
  var seen_nodes = {},
      to_visit = [],
      edges = {};

  seen_nodes[node.id] = node;
  to_visit.push(node);

  while (to_visit.length > 0) {
    var node = to_visit.pop();
    var connections = node.get_connections();

    connections.points_to.forEach(function(to) {
        edges[node.id] = to.id;
        if (!seen_nodes[to.id]) {
            seen_nodes[to.id] = to;
            to_visit.push(to);
        }
    });

    connections.pointed_by.forEach(function(by) {
        edges[node.id] = by.id;
        if (!seen_nodes[by.id]) {
            seen_nodes[by.id] = by;
            by_visit.push(by);
        }
    });
  }

  return {
    nodes: seen_nodes,
    edges: edges
  };
}
```

There are two many details there that can distract us from the principal algorithm.
Let's rewrite it slightly by omitting the assumtpion that we control
storage:

``` javascript
function traverse(first_node) {
  api.init(first_node);
  store.init();

  var next_node;

  while (next_node = api.getNextNode()) {
    var edges = api.getEdges(next_node);
    var api.markVisited(next_node)

    store.addNodesAndEdges(edges);
    api.extractNodesToVisit(edges);
  }

  return store.extractGraph();
}
```

Much nicer now! What this shows is that all the functions
introduced were implemented with the help of local data structures
initially, but we can easily abstract away all logic operations
and make it storage independent. Why would we do that?

For example, it can be that node and edges discovery functions
actually interact with remote service and storage is also
either remote or at least durable, no none of it is o(1) now.

And to make things just a bit more complicated, imagine that
graph is big, like million nodes big and some much bigger number
of edges.

Having this in mind an idea of splitting this logic into separate
threads doesn't sound that bad and that's what Traveller does.

Idea is the following: make graph crawler that will have four threads:

* Download thread
* Processing thread
* Controller thread
* Kickstart thread

Download thread is bothered only with fetching information about
new nodes, processing thread parses incoming information and schedules
new nodes for discovery. Controller thread oversees the hwole thing
and terminates the whole circus when all of them do not have any
work to do. What's kickstarter thread? If graph is disconnected,
traversing one part of it will not result in traversing the whole
graph and kickstarter is just a place where we can manually intervene
and ask to fetch some more nodes.

You saw quite a lot of words like `fetches` or `schedules`, but how
is it implemented? Traveller actually doesn't care, it's up to you.
If you do it right, you can have *lots* of travellers walking in
all different directions gathering the data for you.

Traveller library itself aims only to make algorithm itself as robust
as possible by taking into consideration possible failures on every
single step on it's way.

Have fun!

## Development

For some weird reason cider doesn't load namespaces from the project by
default, hence you need to call `cider-refresh` after `cider-jack-in`.
You can also say `,refresh` in REPL.
