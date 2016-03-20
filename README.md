FIG Tree
========
The **F**ile **I**ndexed-**G**roups **Tree** is an index for a file split into contiguous groups.

Problem
-------
Consider the problem of representing a mutable file in immutable storage. Each write to the file is recorded as a new entry that represents the range of bytes (the _file group_) that was just written. However, reading a range of bytes is difficult to do efficiently because multiple records may need to be read in order to fulfill the request. These records are not guaranteed to be in any particular order. Furthermore, if there are multiple records that contain data for a particular byte index, the most recent one must be used, because it represents the most recent write and therefore the most up-to-date data (the values of the bytes in older records are said to be _stale_). One algorithm to do this is to scan the records in reverse order, taking care to remember the most recent record for each byte, and stopping when such a record has been found for every byte in the range, or when all records have been traversed. The worst case run-time for this algorithm is linear in the number of records in the file, which is unacceptably slow. An algorithm for reading a range of bytes must be at least linear in the size of the file (as opposed to the number of records). Ideally, it should be linear in the number of bytes being read.

Solution
--------
One solution is to maintain a tree that acts as an index on the file. When the index is to be updated, all modified nodes in the tree, and all of their ancestors in the tree, must be copied, because records are read-only.

One tree that is sometimes used as an index is a B+ Tree. However the fact that the leaves of a B+ Tree are linked makes it unsuitable for a copy-on-write index (if the rightmost leaf is modified, then the entire tree must be copied).

Therefore, our solution is based on a B Tree, a self-balancing tree where each node contains a variable number of entries. A vanilla B Tree is not a desirable index for a mutable file represented as a series of immutable diffs because to write a range of k bytes would require k insertions into the tree, one for each byte in the range.

A quick fix would be store in the B Tree one key-value mapping for each range, rather than one key-value mapping for each byte. For each range that is written, an entry is added to the B Tree mapping the first byte in the range to the record containing data for that range. However, writing a large range would still require the removal of all of the ranges it overlaps with, which could be linearithmic in the size of the range.

A FIG Tree is a modified B Tree that efficiently solves this problem. The principle of a FIG Tree is to map ranges to records, instead of individual bytes to records. When a range of bytes (a _file group_) is written, an entry representing that file group is added to the FIG Tree. An entry consists of a range of byte [a, b] mapped to an identifier of the immutable record containing those bytes. A FIG Tree does not delete the stale intermediate ranges contained within the range of bytes written; instead it puts the entry describing the newly written range higher in the tree so that any queries for bytes in the range will find the new range entries first, and will never find the stale mappings.

While simple in principle, this results in some edge cases when reading and writing data, that are addressed below.

Query Algorithm
---------------
Querying a single byte is done in the same way as is done in a B Tree. Starting at the root, check if the queried byte is in one of the entries at that node. If it is, then the algorithm terminates and the record containing the byte has been found. Otherwise, recurse on the appropriate subtree.

This method can be extended to range queries; traverse the subtree normally, making sure to avoid stale entries. This can be done by keeping track of an interval containing the bytes that are valid at each node. Initially, this range is (-infinity, +infinity). If you enter a subtree between entries containing intervals [a, b] and [c, d], then the interval gets restricted to [b + 1, c - 1]. Backtrack up the tree either when you've finished traversing a node, or when you reach the end of the valid interval.

Insertion Algorithm
-------------------
To do an insert, begin by performing the Query Algorithm, with the following modifications. Keep track of the interval containing valid bytes at each node. At each node, prune the node by "trimming it" to the valid range: this means removing all entries and subtrees that lie completely outside the range, and trimming entries that lie partially within the range (if a subtree lies partially outside the range, then don't bother trimming it; you should never have to traverse any subtrees in order to prune a node). It is important to prune each node as described above in order to prevent stale entries from being pushed up the tree on an insert. Furthermore, stop early when you find a node where at least one entry in the node overlaps with the range that you are inserting. If you reach a leaf node without this happening, then just insert the new entry normally and split and push up entries normally as in a B Tree.

If you stopped at a node early, then observe that the entries that overlap with the range you are inserting are consecutive entries in that node (since the entries in each node are in sorted order). Let [x, y] -> Z be the group that you are trying to write, and let [a, b] -> C, [d, e] -> F, [g, h] -> I, and [i, j] -> K be the entries that overlap with it. First, replace all four of the entries in the node with a single entry [x, y] -> Z, whose left subtree is the subtree that used to be to the left of [a, b] -> C, and whose right subtree is the subtree that used to be to the right of [i, j] -> K. The subtrees between [a, b] -> C and [d, e] -> F, [d, e] -> F and [g, h] -> I, and [g, h] -> I and [i, j] -> K, are completely deleted. If x > a, then let the new group [a, x - 1] -> C be the _left continuation_. If x < j, then let the new group [y + 1, j] -> K be the _right continuation_. Insert the left and right continuations, if they exist, into the tree normally; these insertions should be done at the leaves of the tree, and not at some intermediate node (remember to use a and j, rather than x - 1 and y + 1, to compute the valid intervals when pruning the left and right subtrees of the newly inserted group [x, y] -> Z).

Invariants of a FIG Tree
------------------------
1. The entries in each node have intervals that do not overlap.
2. The entries in each node are arranged in sorted order by their intervals.
3. If [p, q] and [r, s] are adjacent entries in a pruned node in a FIG Tree, then an entry containing any byte index in [q + 1, r - 1], if it exists, is guaranteed to be in the subtree between [p, q] and [r, s].
4. The number of entries in any node is smaller than some odd-numbered threshold. If an insert causes this to be violated, then it is immediately split, and the middle element is pushed up to the parent. If there is no parent, then it becomes the root of the tree. (This is exactly the same as a B Tree.)
5. If a byte index x is contained within multiple entries in a FIG Tree, then the entry representing the most recent file group is higher in the tree than the other entries.
6. If all nodes in the FIG Tree are pruned to their valid intervals, then any byte index x is contained within at most one entry in a FIG Tree.