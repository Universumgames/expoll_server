# Important upgrade information

## Normal upgrade

When upgrading, in general, client and server have to be upgraded to the same version excluding the smallest iteration (
like "1.5.x" for example). In this example both client and server have to run any version of "1.5" to properly work.
Upgrading is as simple as you might hope. It's as easy as your first installation, just get the newest code and run
the `./run-full-server.sh` file and everything else should update.

## Version specific steps

Sometimes you have to put real manual labor into the upgrade process. So you cannot just run a simple script and
everything works out just fine.

Here is a list of version where a manual migration according to the `./manual_upgrade_labor/` directory has to be
executed (note that you first need to upgrade to the exact version on the left to properly upgrade):

- 1.5.2 -> 2.0.0 (see `./manual_upgrade_labor/1_5_to_2_0.sql`)
- 2.5.19 -> 3.0.0 Architecture change, no manual labor
