== How data is stored:

1) The list of files to be backed up is put together as a text file, along with the date of the backup.  This is called the backup manifest.
2) Each of the files in the backup manifest are backed up, using the computer name + file name as a label.
3) The backup manifest itself is backed up, using the backup stream as a label.
  * (The backup stream is just a way of naming your backups so you can have multiple different serieses of backups.)


== Types of data stored:
* Immutable data is stored using Content Based Addressing.  The SHA1 hash of the contents of the data is the address of the data.  This data
  is incorruptible because the address is determined by the content, and FB uses a cryptographically secure algorithm to determine the address.

  Types of immutable data:
  * An erasure block is a chunk which can be used to reconstitute a larger piece of data.  Each erasure is stored on only one friend node, but
    not all erasures are necessary to reconstitute the larger piece of data.
  * The erasure manifest is a list of the erasures.  Currently each erasure manifest is stored on every friend node.

* There is only one kind of mutable data: a pointer to a piece of immutable data.  Mutable data is is located by the owner + a label.  A SHA1
  of the combination of owner, label, pointer, and a timestamp is signed with the owner's private key, so this data is also incorruptible.
  The latest timestamp for a particular owner + label is considered the authoritative version.
  * Note that it is possible for a node to return an older version of the label rather than the most recent, so it is possible for another
    user to cause you to get an older version of your data rather than the latest.  Every online user who has the mutable data node
    would have to lie for this to happen.
    * In the future, we could mitigate this risk by letting you associate a one-time label with a piece of data.  Then the only exploit
      (assuming that our military grade encryption is not compromised) would be to make your data vanish.

These are the equivalent of the low level data structures held on your hard drive.  The data you care about, your files and the list
of files that go together to make a backup, are built out of this low level data.


== How a file is backed up:
1) The file to be backed up is broken into 60 erasure blocks, each 1/40th the size of the whole file.
  * Any 40 of these blocks can be used to recover the original file.
2) Each erasure block is stored on a "friend node".
  * The identifier for the data is the SHA1 hash of the contents of the block.
  * The friend is determined by going through your friend list round-robin for each erasure block.
3) An erasure manifest is created, listing the SHA1 hashes of all the erasure blocks.
4) The erasure manifest is stored on all friend nodes.
  * The identifier for the data is the SHA1 hash of the contents of the manifest.
4) A label is created which contains the identifier of the erasure manifest, the owner's identity, the label name itself, and a signature of all the data.
  *  The identifier of the label is the hash of the owner + label name.


== How data is deleted:
Each chunk of data a node holds has a number of leases attached to it.  Each lease tells the leaseholder, the
time the lease expires, whether the lease is hard or soft (i.e. if it is critical to keep the data or just
nice-to-have), and a signature to verify the leaseholder really requested the lease.  You can add or remove
leases to a chunk of data (typically you would do this for all the chunks in a file together).  If all the
leases are removed, the chunk is deleted.  If all the non-soft leases are removed, then the chunk may
or may not be deleted, depending on how much space the node has available.

== How data is encrypted:

== How nodes & the server communicate:
=== Interactions with the server
=== Interactions during a backup
=== Interactions during a restore

== How friends are selected:
