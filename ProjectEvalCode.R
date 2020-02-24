library(data.table)
rankingFile = fread("default.test",header=F)
relevanceFile = fread("train.pages.cbor-article.qrels",header=F)

names(rankingFile) = c("qId","0","docId","rank","score","teamName")
names(relevanceFile) = c("qId","0","docId","relevance")



qIdVector = as.vector(relevanceFile[1,1],"character")
for(i in 1:(nrow(relevanceFile)-1)){
  if(relevanceFile[i,1]!=relevanceFile[i+1,1]) {
    qIdVector = append(qIdVector,as.vector(relevanceFile[i+1,1],"character"))
  }
}



docList = list()
sortedDocList = list()
qIdVector2 = c()
sortedQidVector2 = c()
for(i in 1:length(qIdVector)) {
  docIndexVector = c()
  rankDummy = rankingFile[rankingFile$qId==qIdVector[i],]
  relDummy = relevanceFile[relevanceFile$qId==qIdVector[i],]
  for(j in 1:nrow(relDummy)) {
    for(k in 1:nrow(rankDummy)) {
      if(rankDummy$docId[k]==relDummy$docId[j]) {
        docIndexVector = append(docIndexVector,k)
      }
    }
  }
  if(!(is.null(docIndexVector))) {
    docList[[i]] = docIndexVector
    qIdVector2 = append(qIdVector2,qIdVector[i])
    
  }
}

precList = list()
precisionAtR = function(qVector2,dList,pList) {
  for(i in 1:length(dList)) {
    tpVector = c()
    for(j in 1:length(dList[[i]])) {
      tp = j
      tpVector = append(tpVector,tp/length(relevanceFile[relevanceFile$qId==qVector2[i],]$docId))
    }
    pList[[i]] = tpVector
  }
  for(i in 1:length(pList)) {
    x = cbind("For query: ",i)
    print(x)
    print(pList[[i]])
  }
}
print("Precision@R for each query:")
precisionAtR(qIdVector2,docList,precList)

meanAvgPrec = function(qVector2,dList) {
  totalAq = 0
  for(i in 1:length(dList)) {
    sum = 0
    tpVector = c()
    for(j in 1:length(dList[[i]])) {
      tp = j
      sum = sum + (tp/length(relevanceFile[relevanceFile$qId==qVector2[i],]$docId))
    }
    sum = sum /length(dList[[i]])
    totalAq = totalAq + sum
  }
  map = totalAq / length(dList)
  print(map)
}
print("MAP:")
meanAvgPrec(qIdVector2,docList)

docList = list()
sortedDocList = list()
#qIdVector2 = c()
sortedQidVector2 = c()
for(i in 1:length(qIdVector)) {
  docRankVector = rankingFile[rankingFile$qId==qIdVector[i],]$rank
  sortedDocRankVector = rev(docRankVector)
  docList[[i]] = docRankVector
  sortedDocList[[i]] = sortedDocRankVector
}
#print("list:")
#print(docList[[1]][1])
#print("sorted list:")
#print(sortedDocList[[1]][1])
nDcg = function(qvector,dList1,dList2) {
  sum = 0;
  for(i in 1:length(dList1)) {
    dcg = 0
    for(j in 2:5) {
      if(j > length(dList1[[i]])) {
        break
      }
      dcg = dcg + (2^(dList1[[i]][j])/log2(dList1[[i]][j]+1))
      #datprint(dcg)
    }
    dcgi = 0
    for(k in 1:4) {
      if(k > length(dList2[[i]])) {
        break
      }
      #print("sorted doc list")
      #print(dList2[[i]][k])
      
      dcgi = dcgi + (2^(dList2[[i]][k])/log2(dList2[[i]][k]+1))
      
      #print(dcgi)
    }
    sum = sum + ((1/dcgi)*dcg)
  }
  return(sum/length(qvector))
}
print("NDCG: ")
nDcg(qIdVector,docList,sortedDocList)