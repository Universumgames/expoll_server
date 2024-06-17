#!/bin/sh

# source swagger file
sourceFile=$1
# target file
targetFile=$2

getLeadingSpaces() {
  string=$1
  # Remove everything after the first non-space character
  trimmed="${string#"${string%%[![:space:]]*}"}"
  # Count the length of the trimmed string
  leading_spaces=$(( ${#string} - ${#trimmed} ))
  echo $leading_spaces
}

# function to put line in target file or resolve reference
putLine() {
  # get line
  local line=$1
  local preIndent=$2
  local sourceDir=$3
  # check if line is a reference (beginning with "$ref:")
  # shellcheck disable=SC2039
  if [[ $line == *\$ref* ]] && [[ $line != *\"#* ]]; then
    echo "Resolving reference line: $line"
    # get reference
    reference=$(echo $line | awk '{print $2}' | tr -d '"')
    echo "Resolving reference: $reference"
    # get reference file
    referenceFile=$sourceDir/$reference
    echo "Resolving reference file: $referenceFile"
    # count spaces before first normal character
    local indent=$(($(getLeadingSpaces $line) + $preIndent))
    # read reference file
    echo "Reading reference file: $referenceFile with indent: $indent"
    readFile $referenceFile $indent
  else
    # put line in target file with preIndent
    line="$(printf "%${preIndent}s")$line"
    echo $line
    echo $line >> $targetFile
  fi
}

readFile() {
  # get file
  local file=$1
  local fileDir=$(dirname $file)
  echo "Reading file: $file"
  echo "Reading file dir: $fileDir"
  local preIndent=$2
  IFS=''
  # read file line by line
  while read  line; do
    # put line in target file
    #echo $fileDir
    putLine "$line" $preIndent $fileDir
  done < $file
}

# create a new file
echo "" > $targetFile

# read source file
readFile $sourceFile 0