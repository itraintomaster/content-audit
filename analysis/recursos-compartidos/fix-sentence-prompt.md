# Fix sentence Prompt
You are part of a pipeline for improving the quiz sentences of an English learning course. 

For doing so, I will pass you a json like this: 
{
    "knowledgeId" : "6814dafa7d73e7209a13d384",
    "quizId" : "67fab6d59930102295341e5f",
    "title": "Affirmative sentences with 'be' in the present simple",
    "instructions": "Escribe la forma afirmativa en presente simple del verbo 'be'. No uses contracciones (short forms).",
    "topicName": "Present Simple",
    "quizSentence": "She ____ [is] English.",
    "sentence": "She is English.",
    "translation": "Ella es inglesa.",
    "tokensToRemove" : [ "English" ],
    "reason" : "Contains words from a higher CEFR level: English",
    "level" : "A1",
    "incorrectLevel" : "A2",
    "suggestedWords": [
        {
        "lemma" : "as",
        "partOfSpeech" : "",
        "importance" : 0
      }, {
        "lemma" : "than",
        "partOfSpeech" : "preposition",
        "importance" : 0
      }, {
        "lemma" : "also",
        "partOfSpeech" : "adverb",
        "importance" : 0
      }, {
        "lemma" : "between",
        "partOfSpeech" : "adverb",
        "importance" : 1
      }, {
        "lemma" : "problem",
        "partOfSpeech" : "noun",
        "importance" : 1
      }, {
        "lemma" : "something",
        "partOfSpeech" : "pronoun",
        "importance" : 1
      }, {
        "lemma" : "same",
        "partOfSpeech" : "adjective",
        "importance" : 1
      }, {
        "lemma" : "world",
        "partOfSpeech" : "noun",
        "importance" : 1
      }, {
        "lemma" : "ask",
        "partOfSpeech" : "verb",
        "importance" : 1
      }
    ]
}

And you will try to generate a new sentence following the instructions of the excercise, not using the tokensToRemove and using one or some lemmas on "suggestedWords". For example:

{
    "quizSentence": "She ____ [is] the problem.",
    "sentence": "She is the problem."
    "translation": "Ella es el problema.",
    "wordsUsed" : [  
      {
        "lemma" : "problem",
        "partOfSpeech" : "noun",
        "importance" : 1
      }
    ]
}

Is very important to follow the instruction rules. Also, try to preserve the response (except the case the response is the token to remove), in the example [is]. Please take into account that the newly created sentence should be pedagogically correct and meaninful. 
- Pay attention to the "level" of the quiz. Avoid making sentences that fall outside its level. For instance, "She is a teacher" is good for A1, but "She is the thing" sound little weird for a starting level. 
- Make sure your sentence make sense with the title and instructions. 
- If possible, preserve the length of the sentence (but it is secondary).
- If possible, be possitive. For example, if you have "She ____ [doesn't] (do not) like pasta" and you have "son", "house", it is better "She ____ [doesn't] (do not) like the house" over than "She ____ [doesn't] (do not) like his son". It's a little rude to say "I don't like your son".
- Please, make sure the sentence is pragmatic. With this we mean the sentence is something usable in real and common contexts.

Is very important to use always the same JSON response:
{
    "quizSentence": string,
    "sentence": string,
    "translation": string,
    "wordsUsed" : [  
      {
        "lemma" : string,
        "partOfSpeech" : string,
        "importance" : integer
      }
    ]
}

Let's try with this:

%s