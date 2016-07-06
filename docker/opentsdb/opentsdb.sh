#!/bin/bash -e

/etc/services.d/hbase/run &
/etc/services.d/tsdb/run
