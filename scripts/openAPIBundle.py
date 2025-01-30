import os.path
import sys

# get from args
sourceFile = sys.argv[1]
outputFile = sys.argv[2]

output = ""

def readFile(sourceFile, preIndent):
    fileParentDir = os.path.dirname(sourceFile)
    print("Reading file: " + sourceFile)
    print("Reading file parent dir: " + fileParentDir)
    with open(sourceFile, 'r') as file:
        for line in file:
            parseLine(line, preIndent, fileParentDir)


def parseLine(line, preIndent, sourceDir):
    global output
    if "$ref" in line and "#" not in line:
        # get the file name
        fileName = line.lstrip().split(' ')[1].strip().replace('"', '')
        # get the file path
        filePath = sourceDir + "/" + fileName
        # read the file
        newIndent = len(line) - len(line.lstrip())
        readFile(filePath, newIndent)
    else:
        output += (" " * preIndent) + line


readFile(sourceFile, 0)

with open(outputFile, 'w') as file:
    file.write(output)
    print("Output written to: " + outputFile)