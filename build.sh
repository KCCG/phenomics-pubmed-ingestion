#!/bin/bash
set -e

: ${1?"Usage: $0 <zipfile>"}

zipfile=$1

aws s3api put-object --bucket phenomics-artifacts --key $zipfile  --body build/distributions/$zipfile
