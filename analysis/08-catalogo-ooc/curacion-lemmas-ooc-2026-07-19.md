# Curación de lemmas fuera-de-catálogo — 2026-07-19

Fuente: audit más reciente post-fixes (419 lemmas, 779 ocurrencias penalizadas).
Objetivo: decidir qué palabras se AGREGAN al catálogo enriquecido (dejan de penalizar y de generar tareas LEMMA_ABSENCE) y cuáles quedan como señal genuina.

Leyenda: **agregar** (curada a mano) · **agregar-auto** (nivel por banda de frecuencia, revisar) · **metalenguaje** (términos gramaticales de drills → A1) · **artefacto** (lema mal generado por spaCy; se agrega el artefacto con el nivel de la palabra real) · **no-agregar** (señal genuina o infrecuente) · **nombre-propio** (ruido residual aceptado)


## 1. Agregar (curadas a mano) (41 lemmas, 208 ocurrencias)

| lemma | nivel | n | rank | niveles quiz | ejemplo | nota |
|---|---|---|---|---|---|---|
| excited | A1 | 13 | 2696 | A1,A2 | I am not excited. | EVP real la lista A1; el catálogo enriquecido la perdió |
| hike | B1 | 13 | 5253 | A2,B1,B2 | We've hiked in forests. |  |
| swimming | A1 | 12 | 5357 | A1,B1,B2 | swim - swimming | gerundio/actividad; swim es A1 |
| videogame | A2 | 12 | 16830 | A1,A2,B1,B2 | We 're going to play videogames together. | grafía junta de video game (A2) |
| soda | A2 | 11 | 4875 | A1,A2,B1 | Why does he drink much soda? |  |
| vacation | A1 | 9 | 17896 | A1,A2 | - last month I had a vacation. | variante AmE de holiday (A1) |
| bored | A1 | 9 | 5607 | A1,A2,B1,B2 | She is sometimes bored. |  |
| delighted | B1 | 7 | 7462 | A2,B1,B2 | She was delighted to see the penny. |  |
| italian | A1 | 6 | 3251 | A1,A2 | Do they study Italian? | nacionalidad |
| pleased | A2 | 6 | 3731 | A2,B1 | We are very pleased with your earrings. |  |
| crowded | A2 | 5 | 5638 | A2,B1,B2 | The pubs in this city are very crowded. |  |
| boring | A1 | 5 | 3989 | A2,B1 | This is a boring old movie. |  |
| soccer | A1 | 5 | 3548 | A2,B1 | We play soccer once a week. |  |
| german | A1 | 4 | 2221 | A1,A2 | You are German. | nacionalidad |
| airplane | A1 | 4 | 4247 | A1,A2,B1 | He is on the airplane now. | variante AmE de plane (A1) |
| wonderfully | A2 | 4 | 9355 | A1,A2 | Thomas played the guitar wonderfully. | adverbio de wonderful (A1) |
| exhausted | B1 | 4 | 5302 | A2,B1,B2 | A: I feel exhausted. B: I'll make some tea. |  |
| treehouse | A2 | 4 | — | A2,B2 | They built a treehouse. | compuesto infantil frecuente en el curso |
| mistaken | B1 | 4 | 5290 | A2,B1,B2 | We have not mistaken/haven't mistaken the address. | participio de mistake (A2/B1) |
| cycling | A2 | 4 | 8880 | A2,B1,B2 | Cycling is a fun hobby. |  |
| downstairs | A2 | 4 | 5204 | A2,B1 | I spoke with the man who works downstairs . |  |
| playful | B1 | 4 | 9290 | A2,B1,B2 | Your dogs are nice! Some of ours are playful. |  |
| thrilled | B1 | 4 | 5707 | A2,B1,B2 | Are you thrilled about your birthday? |  |
| squirrel | B1 | 4 | 7307 | B1,B2 | The squirrels were chased (by the dogs). | vocabulario de animales |
| comic_strip | A2 | 3 | 4708 | A1,A2,B1 | Does she read many comics? | comic (A2) |
| karaoke | A2 | 3 | 14799 | A1,A2 | Does she do karaoke often? |  |
| spanish | A1 | 3 | 2544 | A1,A2 | He's not Spanish. | nacionalidad |
| scared | A1 | 3 | 2340 | A1,A2 | He wasn't scared. |  |
| flavor | A2 | 3 | 3390 | A1,A2 | Which flavor did you get? |  |
| candy | A1 | 3 | 3370 | A1,A2 | I don't want any candies. |  |
| noisily | A2 | 3 | — | A1,A2 | He closed the door noisily. | adverbio de noisy (A2) |
| skillful | B1 | 3 | 14866 | A1,A2 | He draws skillful houses. |  |
| talented | A2 | 3 | 4025 | A2,B1 | She was a talented artist. |  |
| skiing | A2 | 3 | 6459 | A2 | Has he ever gone skiing? |  |
| motorcycle | A2 | 3 | 6221 | A2,B2 | We need to check oil in each of the motorcycles. |  |
| advisor | B2 | 3 | 5208 | A2,B1,B2 | She is an advisor of hers. |  |
| semester | B1 | 3 | 5501 | A2,B1,B2 | I will be able to read the textbook next semester. |  |
| refreshing | B1 | 3 | 7901 | A2,B1,B2 | This is a refreshing iced tea. |  |
| annoyed | B1 | 3 | 8487 | A2,B1,B2 | I became really annoyed as the customer complained endlessly |  |
| cafe | A1 | 3 | 4751 | B1 | What will she be drinking at the cafe? |  |
| colorful | A2 | 3 | 5392 | B1 | The bird with the colorful feathers was on the tree. |  |

## 2. Metalenguaje de consignas → A1 (3 lemmas, 36 ocurrencias)

| lemma | nivel | n | rank | niveles quiz | ejemplo | nota |
|---|---|---|---|---|---|---|
| uncountable | A1 | 20 | — | A1 | fresh water uncountable | término gramatical usado en drills |
| countable | A1 | 13 | — | A1 | ears plural countable | término gramatical usado en drills |
| pm | A1 | 3 | 1800 | A1 | Was she happy at 5 pm? | notación horaria (5 pm) |

## 3. Artefactos del lematizador (se agrega el lema-artefacto) (4 lemmas, 51 ocurrencias)

| lemma | nivel | n | rank | niveles quiz | ejemplo | nota |
|---|---|---|---|---|---|---|
| texte | A2 | 24 | 13890 | A2,B1,B2 | They haven't texted me. | texted→'texte'; palabra real 'text' (verbo, A2) |
| cooky | A1 | 19 | 2923 | A1,A2 | You are making cookies. | cookies→'cooky'; palabra real 'cookie' (A1) |
| shin | A2 | 5 | 7704 | B1,B2 | When was it shining? | shining→'shin'; palabra real 'shine' (A2) |
| din | B1 | 3 | 8258 | A2,B1,B2 | John is dining out tonight. | dining→'din'; palabra real 'dine' (B1) |

## 4. Agregar con nivel automático por frecuencia (REVISAR) (130 lemmas, 161 ocurrencias)

| lemma | nivel | n | rank | niveles quiz | ejemplo | nota |
|---|---|---|---|---|---|---|
| applicant | B2 | 3 | 5240 | B1,B2 | We interviewed the applicants that applied to the company. | nivel propuesto por banda de frecuencia (revisar) |
| amazed | B2 | 3 | 6402 | B1,B2 | I was amazed that she remembered my birthday. | nivel propuesto por banda de frecuencia (revisar) |
| fashioned | B2 | 3 | 7740 | B1,B2 | He has old-fashioned furniture in his house. It looks like i | nivel propuesto por banda de frecuencia (revisar) |
| british | A2 | 2 | 1439 | A1 | They are not British. | nivel propuesto por banda de frecuencia (revisar) |
| theater | A2 | 2 | 1504 | A1 | I don't go to the theater often. | nivel propuesto por banda de frecuencia (revisar) |
| cement | B2 | 2 | 7051 | A1,A2 | Use a little water with the cement. | nivel propuesto por banda de frecuencia (revisar) |
| married | A2 | 2 | 1669 | A1 | She is married to John. | nivel propuesto por banda de frecuencia (revisar) |
| frightened | B2 | 2 | 6843 | A1,B2 | He is frightened of the night. | nivel propuesto por banda de frecuencia (revisar) |
| depend | A2 | 2 | 1604 | A1 | It depends on the time. | nivel propuesto por banda de frecuencia (revisar) |
| vanilla | B2 | 2 | 6107 | A1,B1 | I'd like a vanilla milkshake, please. | nivel propuesto por banda de frecuencia (revisar) |
| beverage | B2 | 2 | 6903 | A2,B1 | She likes this hot beverage. | nivel propuesto por banda de frecuencia (revisar) |
| gardening | B2 | 2 | 7714 | A2,B1 | They did the gardening. | nivel propuesto por banda de frecuencia (revisar) |
| skating | B2 | 2 | 6792 | A2,B1 | He spent a little money skating. | nivel propuesto por banda de frecuencia (revisar) |
| swum | B1 | 2 | 3163 | A2 | Has Peter swum to the island? | nivel propuesto por banda de frecuencia (revisar) |
| coke | B2 | 2 | 5751 | A2,B2 | Let's not drink coke tonight. | nivel propuesto por banda de frecuencia (revisar) |
| cooking | B1 | 2 | 3764 | A2,B1 | Cooking is very easy. | nivel propuesto por banda de frecuencia (revisar) |
| cute | A2 | 2 | 2760 | A2 | They looked at the cute puppy. | nivel propuesto por banda de frecuencia (revisar) |
| hiking | B2 | 2 | 5230 | B1,B2 | Hiking is getting more trendy. | nivel propuesto por banda de frecuencia (revisar) |
| freshly | B2 | 2 | 6363 | B1 | He drank the juice that was freshly squeezed. | nivel propuesto por banda de frecuencia (revisar) |
| lipstick | B2 | 2 | 7800 | B1,B2 | She ought to apply lipstick. | nivel propuesto por banda de frecuencia (revisar) |
| qualified | B2 | 2 | 5621 | B1,B2 | All the teachers are well-qualified. They all hold doctorate | nivel propuesto por banda de frecuencia (revisar) |
| cigar | B2 | 2 | 6600 | B1,B2 | He still smokes cigars. | nivel propuesto por banda de frecuencia (revisar) |
| librarian | B2 | 2 | 5154 | B1,B2 | We volunteer as librarians. | nivel propuesto por banda de frecuencia (revisar) |
| graduation | B2 | 2 | 5001 | B2 | Laura had visited us since graduation. | nivel propuesto por banda de frecuencia (revisar) |
| smartphone | B2 | 2 | 5887 | B2 | A smartphone costs a lot. | nivel propuesto por banda de frecuencia (revisar) |
| brag | B2 | 2 | 7406 | B2 | The girl bragged that she had done everything by herself. | nivel propuesto por banda de frecuencia (revisar) |
| stove | B2 | 2 | 5457 | B2 | The tea that is on the stove is too hot. that is on the stov | nivel propuesto por banda de frecuencia (revisar) |
| isolated | B2 | 2 | 5347 | B2 | There are some isolated people in New York. | nivel propuesto por banda de frecuencia (revisar) |
| labor | A2 | 1 | 1314 | A1 | They labor on the beach. | nivel propuesto por banda de frecuencia (revisar) |
| taxis | B2 | 1 | 5188 | A1 | He does not drive taxis. | nivel propuesto por banda de frecuencia (revisar) |
| genre | B1 | 1 | 3981 | A1 | What genre does he play? | nivel propuesto por banda de frecuencia (revisar) |
| sheep | B1 | 1 | 4450 | A1 | Sheep run. Verb in infinitive = run | nivel propuesto por banda de frecuencia (revisar) |
| smoking | B2 | 1 | 5325 | A1 | smoke - smoking | nivel propuesto por banda de frecuencia (revisar) |
| dancing | A2 | 1 | 1958 | A1 | dance - dancing | nivel propuesto por banda de frecuencia (revisar) |
| admitting | A2 | 1 | 1121 | A1 | admit - admitting | nivel propuesto por banda de frecuencia (revisar) |
| irish | B1 | 1 | 3465 | A1 | Are we Irish? | nivel propuesto por banda de frecuencia (revisar) |
| chinese | A2 | 1 | 1417 | A1 | We 'll eat Chinese food tonight. | nivel propuesto por banda de frecuencia (revisar) |
| painting | A2 | 1 | 1559 | A1 | He made four paintings yesterday! | nivel propuesto por banda de frecuencia (revisar) |
| clean | A2 | 1 | 1574 | A1 | Clean the knife. | nivel propuesto por banda de frecuencia (revisar) |
| european | A2 | 1 | 1528 | A1 | She loves European countries. | nivel propuesto por banda de frecuencia (revisar) |
| lot | A2 | 1 | 1501 | A1 | The school had lots of computers. | nivel propuesto por banda de frecuencia (revisar) |
| indian | A2 | 1 | 1931 | A1 | There is an Indian takeaway near here. | nivel propuesto por banda de frecuencia (revisar) |
| perch | B2 | 1 | 7619 | A1 | The bird was on a perch. | nivel propuesto por banda de frecuencia (revisar) |
| singing | A2 | 1 | 1066 | A1 | Singing is difficult for him. | nivel propuesto por banda de frecuencia (revisar) |
| daddy | A2 | 1 | 2041 | A1 | Look at me, daddy! | nivel propuesto por banda de frecuencia (revisar) |
| grocery | B1 | 1 | 3420 | A2 | We carried the groceries into the house. | nivel propuesto por banda de frecuencia (revisar) |
| favor | B1 | 1 | 3057 | A2 | She favored to go to the cinema. | nivel propuesto por banda de frecuencia (revisar) |
| garbage | B1 | 1 | 3904 | A2 | She threw the garbage. | nivel propuesto por banda de frecuencia (revisar) |
| app | A2 | 1 | 2126 | A2 | We kept using the app. | nivel propuesto por banda de frecuencia (revisar) |
| spelling | B2 | 1 | 7276 | A2 | Has he checked the spelling? | nivel propuesto por banda de frecuencia (revisar) |
| skated | B2 | 1 | 6792 | A2 | She 's skated on the ice. | nivel propuesto por banda de frecuencia (revisar) |
| traveler | B1 | 1 | 4359 | A2 | The travelers liked eating the curry. | nivel propuesto por banda de frecuencia (revisar) |
| entertaining | B2 | 1 | 5793 | A2 | Dancing is very entertaining. | nivel propuesto por banda de frecuencia (revisar) |
| challenging | B1 | 1 | 4740 | A2 | Skiing is really challenging. | nivel propuesto por banda de frecuencia (revisar) |
| shouting | A2 | 1 | 2473 | A2 | No shouting! | nivel propuesto por banda de frecuencia (revisar) |
| hunting | B1 | 1 | 3344 | A2 | No hunting! | nivel propuesto por banda de frecuencia (revisar) |
| chunk | B1 | 1 | 4860 | A2 | He had a chunk of cheddar. | nivel propuesto por banda de frecuencia (revisar) |
| gallon | B1 | 1 | 4682 | A2 | He gulped a gallon of milk. | nivel propuesto por banda de frecuencia (revisar) |
| honorable | B2 | 1 | 7531 | A2 | He seems like an honorable man. | nivel propuesto por banda de frecuencia (revisar) |
| attic | B2 | 1 | 7788 | A2 | That is a box for the attic. | nivel propuesto por banda de frecuencia (revisar) |
| scanner | B2 | 1 | 7900 | A2 | This is a scanner. | nivel propuesto por banda de frecuencia (revisar) |
| situate | B2 | 1 | 7134 | A2 | Edinburgh is situated in the Scotland. | nivel propuesto por banda de frecuencia (revisar) |
| napkin | B2 | 1 | 7813 | A2 | Did Michael hand us these napkins . | nivel propuesto por banda de frecuencia (revisar) |
| costly | B2 | 1 | 5110 | A2 | Most software is costly. | nivel propuesto por banda de frecuencia (revisar) |
| deserted | B2 | 1 | 7588 | A2 | I saw nobody at the park. It was nearly deserted. | nivel propuesto por banda de frecuencia (revisar) |
| nanny | B2 | 1 | 7457 | A2 | The nanny was nice to the kids. | nivel propuesto por banda de frecuencia (revisar) |
| engagement | B1 | 1 | 3263 | A2 | They invited us to their engagement party. | nivel propuesto por banda de frecuencia (revisar) |
| catalog | B2 | 1 | 5471 | A2 | Do you have a catalog of steaks? | nivel propuesto por banda de frecuencia (revisar) |
| prom | B2 | 1 | 7946 | A2 | Would she dance at the prom? | nivel propuesto por banda de frecuencia (revisar) |
| rose | A2 | 1 | 2795 | A2 | We bought some small blue roses. | nivel propuesto por banda de frecuencia (revisar) |
| wetter | A2 | 1 | 2856 | A2 | The soup is wetter than the bread. | nivel propuesto por banda de frecuencia (revisar) |
| gripping | B2 | 1 | 6121 | A2 | I'd like a more gripping story. | nivel propuesto por banda de frecuencia (revisar) |
| ugly | B1 | 1 | 3019 | A2 | This shirt is - ugliest. | nivel propuesto por banda de frecuencia (revisar) |
| turtle | B1 | 1 | 4702 | A2 | The turtle crawled slowly. | nivel propuesto por banda de frecuencia (revisar) |
| sometime | B1 | 1 | 4686 | B1 | Do visit us sometime! | nivel propuesto por banda de frecuencia (revisar) |
| hailing | B2 | 1 | 6081 | B1 | It was not hailing/wasn't hailing. | nivel propuesto por banda de frecuencia (revisar) |
| cafeteria | B2 | 1 | 7849 | B1 | The teachers are in the cafeteria, aren't they? | nivel propuesto por banda de frecuencia (revisar) |
| sibling | B1 | 1 | 4533 | B1 | My siblings aren't at home, are they? | nivel propuesto por banda de frecuencia (revisar) |
| urgency | B2 | 1 | 7041 | B1 | The press doesn't / don't don't understand the urgency! | nivel propuesto por banda de frecuencia (revisar) |
| orbit | B1 | 1 | 4548 | B1 | Would you like to travel into - orbit? | nivel propuesto por banda de frecuencia (revisar) |
| jewelry | B1 | 1 | 4792 | B1 | She keeps a little - jewelry. | nivel propuesto por banda de frecuencia (revisar) |
| detain | B2 | 1 | 6971 | B1 | She is detained (by the security guard). | nivel propuesto por banda de frecuencia (revisar) |
| maid | B2 | 1 | 6044 | B1 | The bathroom is being cleaned by the maid. | nivel propuesto por banda de frecuencia (revisar) |
| unlocked | B2 | 1 | 5951 | B1 | The doors might be unlocked. | nivel propuesto por banda de frecuencia (revisar) |
| receiver | B1 | 1 | 3564 | B1 | Would you mind picking up the receiver? | nivel propuesto por banda de frecuencia (revisar) |
| originate | B2 | 1 | 5568 | B1 | I enjoy the stories that originate from old relationships. | nivel propuesto por banda de frecuencia (revisar) |
| closet | B1 | 1 | 3591 | B1 | That jacket which is in the closet belongs to Peter. which i | nivel propuesto por banda de frecuencia (revisar) |
| rattle | B2 | 1 | 6193 | B1 | The baby whose rattle broke began laughing. | nivel propuesto por banda de frecuencia (revisar) |
| soften | B2 | 1 | 5973 | B1 | If you heat butter, it softens. | nivel propuesto por banda de frecuencia (revisar) |
| multiply | B2 | 1 | 6462 | B1 | You don't receive ten if you multiply five and three. | nivel propuesto por banda de frecuencia (revisar) |
| mathematic | B1 | 1 | 4160 | B1 | Do you believe mathematics is perplexing? | nivel propuesto por banda de frecuencia (revisar) |
| astonishing | B2 | 1 | 7424 | B1 | That's astonishing information! | nivel propuesto por banda de frecuencia (revisar) |
| startled | B2 | 1 | 6497 | B1 | He felt startled when the dog barked suddenly. | nivel propuesto por banda de frecuencia (revisar) |
| greek | B1 | 1 | 4044 | B1 | They visited a small Greek village. | nivel propuesto por banda de frecuencia (revisar) |
| artwork | B2 | 1 | 6283 | B1 | The artwork is getting more and more colorful. | nivel propuesto por banda de frecuencia (revisar) |
| confusing | B2 | 1 | 5653 | B1 | The questions are getting more and more confusing. | nivel propuesto por banda de frecuencia (revisar) |
| hallway | B1 | 1 | 4161 | B1 | We tiptoed past the weak woman in the hallway. | nivel propuesto por banda de frecuencia (revisar) |
| quantum | B2 | 1 | 5028 | B1 | I don't really understand quantum physics. | nivel propuesto por banda de frecuencia (revisar) |
| sprint | B2 | 1 | 7966 | B1 | He doesn't run sprints any longer. | nivel propuesto por banda de frecuencia (revisar) |
| devoted | B1 | 1 | 3905 | B1 | She was devoted to her father. | nivel propuesto por banda de frecuencia (revisar) |
| clerk | B1 | 1 | 4427 | B1 | He acts as a clerk. | nivel propuesto por banda de frecuencia (revisar) |
| merchant | B2 | 1 | 5027 | B2 | Merchants trade along the Spice Route. | nivel propuesto por banda de frecuencia (revisar) |
| compartment | B2 | 1 | 7734 | B2 | I get on the train and there's a snake in my compartment! | nivel propuesto por banda de frecuencia (revisar) |
| controller | B2 | 1 | 6329 | B2 | He's been playing that game. There are controllers on the fl | nivel propuesto por banda de frecuencia (revisar) |
| vibrant | B2 | 1 | 6921 | B2 | Never had she visited a city so vibrant. | nivel propuesto por banda de frecuencia (revisar) |
| drill | B2 | 1 | 5146 | B2 | The coach made the players practise the drill. | nivel propuesto por banda de frecuencia (revisar) |
| recite | B2 | 1 | 7223 | B2 | He recited a poem to his son. | nivel propuesto por banda de frecuencia (revisar) |
| massage | B2 | 1 | 7093 | B2 | Having trained all week, she wanted to have a massage. | nivel propuesto por banda de frecuencia (revisar) |
| spectacle | B2 | 1 | 6689 | B2 | She owns several spectacles. not correct | nivel propuesto por banda de frecuencia (revisar) |
| scarve | B2 | 1 | 6607 | B2 | The silk scarves that they sell in that boutique are elegant | nivel propuesto por banda de frecuencia (revisar) |
| millionaire | B2 | 1 | 7008 | B2 | He visited a gallery that is owned by a millionaire. | nivel propuesto por banda de frecuencia (revisar) |
| rotate | B2 | 1 | 5918 | B2 | I need to get the tires rotated. | nivel propuesto por banda de frecuencia (revisar) |
| cellphone | B2 | 1 | 7614 | B2 | The students aren't supposed to use their cellphones during  | nivel propuesto por banda de frecuencia (revisar) |
| premiere | B2 | 1 | 6291 | B2 | He enjoyed the premiere. He should have booked the tickets e | nivel propuesto por banda de frecuencia (revisar) |
| reopen | B2 | 1 | 6965 | B2 | The shop that we visited has reopened. that we visited | nivel propuesto por banda de frecuencia (revisar) |
| dispatch | B2 | 1 | 8000 | B2 | He read the letters - the clerk dispatched. | nivel propuesto por banda de frecuencia (revisar) |
| flock | B2 | 1 | 7107 | B2 | August is the month when tourists flock to the beaches. | nivel propuesto por banda de frecuencia (revisar) |
| terrified | B2 | 1 | 5643 | B2 | That's the spider I'm terrified of. | nivel propuesto por banda de frecuencia (revisar) |
| gaming | B2 | 1 | 6385 | B2 | She has three laptops, one of which is for gaming. | nivel propuesto por banda de frecuencia (revisar) |
| drip | B2 | 1 | 6845 | B2 | I dripped juice on myself and I had to wipe my t-shirt. | nivel propuesto por banda de frecuencia (revisar) |
| flashlight | B2 | 1 | 6978 | B2 | If it were dark, we'd use a flashlight. | nivel propuesto por banda de frecuencia (revisar) |
| quilt | B2 | 1 | 7392 | B2 | She spread the warm blue quilt on the bed. | nivel propuesto por banda de frecuencia (revisar) |
| prospective | B2 | 1 | 5903 | B2 | Sarah is our prospective housemate. | nivel propuesto por banda de frecuencia (revisar) |
| incoming | B2 | 1 | 6621 | B2 | She is our incoming supervisor. | nivel propuesto por banda de frecuencia (revisar) |
| remotely | B2 | 1 | 7655 | B2 | She works remotely on account of her manager. | nivel propuesto por banda de frecuencia (revisar) |
| cutter | B2 | 1 | 7712 | B2 | She cut the fabric with a rotary cutter. | nivel propuesto por banda de frecuencia (revisar) |
| evacuate | B2 | 1 | 6655 | B2 | Luckily, they evacuated from the flooded house. | nivel propuesto por banda de frecuencia (revisar) |
| subscribe | B2 | 1 | 5383 | B2 | He subscribed to a lot of social media channels. | nivel propuesto por banda de frecuencia (revisar) |
| craving | B2 | 1 | 7407 | B2 | She definitely has a craving for chocolate. | nivel propuesto por banda de frecuencia (revisar) |
| comprehension | B2 | 1 | 5800 | B2 | Do you have a good comprehension of his view? | nivel propuesto por banda de frecuencia (revisar) |

## 5. Nombres propios escapados del filtro (no agregar) (1 lemmas, 3 ocurrencias)

| lemma | nivel | n | rank | niveles quiz | ejemplo | nota |
|---|---|---|---|---|---|---|
| emily | — | 3 | — | A2,B1,B2 | A: Is Emily walking? B: Yes, she is. | spaCy no lo taggeó PROPN; ruido residual aceptado (3 casos) |

## 6. NO agregar — señal genuina / infrecuentes (240 lemmas, 320 ocurrencias)

| lemma | nivel | n | rank | niveles quiz | ejemplo | nota |
|---|---|---|---|---|---|---|
| adore | — | 12 | 7135 | A2,B1,B2 | She adored the drink. | formal/C1 — 'She adored the drink' en A2 es justo lo que hay que revisar |
| reside | — | 10 | 5333 | A2,B1,B2 | He loves residing in - Vietnam. | formal/C1 — 'residing' en A2 es señal genuina |
| bloom | — | 7 | 19212 | A2,B1,B2 | A: Are the flowers blooming? B: No, they aren't. | B2/C1 |
| concur | — | 5 | 9595 | A2,B1,B2 | We concurred to dress them. | C2 |
| ace | — | 5 | 7950 | A2,B1,B2 | She aced her test. | informal B2+ como verbo |
| disembark | — | 4 | — | A2,B1 | She disembarked in France. | C1 |
| mow | — | 4 | 10359 | B1,B2 | He painted the star and she mowed the lawn. simultaneous | B2 |
| trim | — | 3 | 5893 | A2,B1,B2 | He wants to trim those bushes . | B2 |
| polish | — | 3 | 8927 | A2,B1,B2 | She polished all my glasses. | B2 como verbo |
| lodge | — | 3 | 8132 | A2,B1,B2 | He is lodging there until Tuesday. | B2/C1 como verbo |
| breech | — | 3 | 19541 | B1,B2 | She owns three breeches. not correct | quiz de formas incorrectas a propósito ('not correct') |
| takeout | — | 3 | 14617 | B1,B2 | Cooking meals is more exciting than ordering takeout! | infrecuente o sin ranking — se deja penalizada (señal) |
| barbershop | — | 3 | 17950 | B2 | I greeted John at the barbershop. | infrecuente o sin ranking — se deja penalizada (señal) |
| usb | — | 2 | 12184 | A1,B1 | We keep our photos on a USB. | infrecuente o sin ranking — se deja penalizada (señal) |
| milkshake | — | 2 | — | A1,B2 | I'd like a vanilla milkshake, please. | infrecuente o sin ranking — se deja penalizada (señal) |
| sweetly | — | 2 | 15062 | A1,A2 | She enunciated sweetly. | infrecuente o sin ranking — se deja penalizada (señal) |
| gracefully | — | 2 | 12469 | A1,B1 | She dances gracefully. | infrecuente o sin ranking — se deja penalizada (señal) |
| sushi | — | 2 | 10088 | A2,B1 | We like sushi and pizza. | infrecuente o sin ranking — se deja penalizada (señal) |
| pertain | — | 2 | 11717 | A2,B1 | That book doesn't pertain to me. | infrecuente o sin ranking — se deja penalizada (señal) |
| salsa | — | 2 | 9964 | A2,B1 | Did they dance salsa? | infrecuente o sin ranking — se deja penalizada (señal) |
| snail | — | 2 | 10666 | A2,B1 | Have you ever cooked snails? | infrecuente o sin ranking — se deja penalizada (señal) |
| detest | — | 2 | 16612 | A2 | I detest driving at night. | infrecuente o sin ranking — se deja penalizada (señal) |
| hydrated | — | 2 | 19340 | A2,B1 | She drinks water to stay hydrated. | infrecuente o sin ranking — se deja penalizada (señal) |
| gateway | — | 2 | 9920 | A2,B1 | Where's the gateway to the garden? | infrecuente o sin ranking — se deja penalizada (señal) |
| tango | — | 2 | 12864 | A2 | She will be able to dance tango after practice next week. | infrecuente o sin ranking — se deja penalizada (señal) |
| fluffy | — | 2 | 10444 | A2,B1 | We found a fluffy white rabbit. | infrecuente o sin ranking — se deja penalizada (señal) |
| thrilling | — | 2 | 10525 | A2,B1 | It's a very thrilling adventure! | infrecuente o sin ranking — se deja penalizada (señal) |
| captivating | — | 2 | 17305 | A2,B1 | That was a very captivating story. | infrecuente o sin ranking — se deja penalizada (señal) |
| effortlessly | — | 2 | 14235 | A2,B2 | She speaks Russian effortlessly. | infrecuente o sin ranking — se deja penalizada (señal) |
| narrate | — | 2 | 10543 | B1,B2 | Have they narrated the story? | infrecuente o sin ranking — se deja penalizada (señal) |
| hamburger | — | 2 | 8570 | B1 | We haven't been eating hamburgers. | infrecuente o sin ranking — se deja penalizada (señal) |
| pinch | — | 2 | 8563 | B1,B2 | He pinched him him / himself on the leg. | infrecuente o sin ranking — se deja penalizada (señal) |
| terrifying | — | 2 | 8327 | B1,B2 | What a terrifying nightmare! It's so real! | infrecuente o sin ranking — se deja penalizada (señal) |
| mop | — | 2 | 14807 | B1,B2 | He cleaned the floor with a mop. | infrecuente o sin ranking — se deja penalizada (señal) |
| sundown | — | 2 | 17385 | B2 | He'll have left by sundown. | infrecuente o sin ranking — se deja penalizada (señal) |
| pollen | — | 2 | 10925 | B2 | The pollen made us sneeze. | infrecuente o sin ranking — se deja penalizada (señal) |
| chirp | — | 2 | 16048 | B2 | I listened to the birds chirp. | infrecuente o sin ranking — se deja penalizada (señal) |
| hoover | — | 2 | — | B2 | Having hoovered the carpet, she sat down for a cup of tea. | infrecuente o sin ranking — se deja penalizada (señal) |
| dungaree | — | 2 | — | B2 | The toddler wore a dungarees. not correct | infrecuente o sin ranking — se deja penalizada (señal) |
| brew | — | 2 | 11733 | B2 | Would you like tea? If so, let's brew a cup. | infrecuente o sin ranking — se deja penalizada (señal) |
| diligently | — | 2 | 14408 | B2 | He studies diligently, so he achieves good grades. | infrecuente o sin ranking — se deja penalizada (señal) |
| remit | — | 1 | — | A1 | My friend remitted money to his family. | infrecuente o sin ranking — se deja penalizada (señal) |
| bouquet | — | 1 | 10760 | A1 | He gave a beautiful bouquet of flowers. | infrecuente o sin ranking — se deja penalizada (señal) |
| sideboard | — | 1 | — | A1 | He put two games on the sideboard. | infrecuente o sin ranking — se deja penalizada (señal) |
| spaghetti | — | 1 | 9650 | A1 | The children ate some spaghetti. | infrecuente o sin ranking — se deja penalizada (señal) |
| sophie | — | 1 | — | A1 | Sophie read from 2pm to. | infrecuente o sin ranking — se deja penalizada (señal) |
| chopstick | — | 1 | 19705 | A1 | He could use chopsticks as a child. | infrecuente o sin ranking — se deja penalizada (señal) |
| sleepily | — | 1 | — | A1 | He sighed sleepily. | infrecuente o sin ranking — se deja penalizada (señal) |
| enunciate | — | 1 | — | A1 | She enunciated sweetly. | infrecuente o sin ranking — se deja penalizada (señal) |
| coldly | — | 1 | 17956 | A1 | He whispered coldly. | infrecuente o sin ranking — se deja penalizada (señal) |
| portuguese | — | 1 | 12273 | A1 | Mary speaks Portuguese well. | infrecuente o sin ranking — se deja penalizada (señal) |
| fret | — | 1 | 11118 | A2 | He fretted about his son. | infrecuente o sin ranking — se deja penalizada (señal) |
| scurry | — | 1 | 11603 | A2 | They scurried to reach the bus. | infrecuente o sin ranking — se deja penalizada (señal) |
| duplicate | — | 1 | 10268 | A2 | He duplicated the files from the computer. | infrecuente o sin ranking — se deja penalizada (señal) |
| bawl | — | 1 | 20075 | A2 | The little girl bawled. | infrecuente o sin ranking — se deja penalizada (señal) |
| lullaby | — | 1 | 18603 | A2 | She sang a lullaby. | infrecuente o sin ranking — se deja penalizada (señal) |
| soundly | — | 1 | 17506 | A2 | The baby fell soundly asleep. | infrecuente o sin ranking — se deja penalizada (señal) |
| sprout | — | 1 | 9527 | A2 | The tree has not sprouted/hasn't sprouted yet. | infrecuente o sin ranking — se deja penalizada (señal) |
| pilate | — | 1 | — | A2 | Have you ever practiced pilates? | infrecuente o sin ranking — se deja penalizada (señal) |
| polo | — | 1 | 9439 | A2 | Have you ever watched polo? | infrecuente o sin ranking — se deja penalizada (señal) |
| cheddar | — | 1 | 17793 | A2 | He had a chunk of cheddar. | infrecuente o sin ranking — se deja penalizada (señal) |
| gulp | — | 1 | 12965 | A2 | He gulped a gallon of milk. | infrecuente o sin ranking — se deja penalizada (señal) |
| omelet | — | 1 | 15938 | A2 | I will prepare an omelet. | infrecuente o sin ranking — se deja penalizada (señal) |
| mailman | — | 1 | 18186 | A2 | She's a mailman. | infrecuente o sin ranking — se deja penalizada (señal) |
| closeby | — | 1 | — | A2 | There is a river closeby. | infrecuente o sin ranking — se deja penalizada (señal) |
| stew | — | 1 | 8400 | A2 | The chefs cook soup and stews. They make the soup here. | infrecuente o sin ranking — se deja penalizada (señal) |
| eraser | — | 1 | 19385 | A2 | I'd prefer those erasers, please . | infrecuente o sin ranking — se deja penalizada (señal) |
| muffin | — | 1 | 8882 | A2 | She cooked some of the carrot muffins. | infrecuente o sin ranking — se deja penalizada (señal) |
| podcast | — | 1 | 8299 | A2 | She listens to no podcasts. | infrecuente o sin ranking — se deja penalizada (señal) |
| toybox | — | 1 | — | A2 | She collected the toys and put each of them into the toybox. | infrecuente o sin ranking — se deja penalizada (señal) |
| bedtime | — | 1 | 9157 | A2 | She kissed each of her daughters before bedtime. | infrecuente o sin ranking — se deja penalizada (señal) |
| domesticate | — | 1 | 17513 | A2 | Not every animal is domesticated. | infrecuente o sin ranking — se deja penalizada (señal) |
| attentively | — | 1 | — | A2 | I listened to him attentively today. | infrecuente o sin ranking — se deja penalizada (señal) |
| teatime | — | 1 | — | A2 | The adults saw nothing at teatime. | infrecuente o sin ranking — se deja penalizada (señal) |
| crosswalk | — | 1 | — | A2 | He waited at the crosswalk. | infrecuente o sin ranking — se deja penalizada (señal) |
| audiobook | — | 1 | — | A2 | We listened to an audiobook while we were running. | infrecuente o sin ranking — se deja penalizada (señal) |
| regretful | — | 1 | — | A2 | Mark was regretful for leaving class. | infrecuente o sin ranking — se deja penalizada (señal) |
| dissimilar | — | 1 | 18140 | A2 | This car is dissimilar from that car. | infrecuente o sin ranking — se deja penalizada (señal) |
| warranty | — | 1 | 9467 | A2 | You should keep the warranty for your laptop. | infrecuente o sin ranking — se deja penalizada (señal) |
| bootcamp | — | 1 | — | A2 | They could write a diary after the bootcamp last year. | infrecuente o sin ranking — se deja penalizada (señal) |
| kg | — | 1 | — | A2 | Robert can lift 100kg. It's great! | infrecuente o sin ranking — se deja penalizada (señal) |
| unattended | — | 1 | 17558 | A2 | The guests mustn't leave their suitcases unattended. | infrecuente o sin ranking — se deja penalizada (señal) |
| iced | — | 1 | 12128 | A2 | This is a refreshing iced tea. | infrecuente o sin ranking — se deja penalizada (señal) |
| hesitant | — | 1 | 10138 | A2 | The cat was hesitant to jump down. | infrecuente o sin ranking — se deja penalizada (señal) |
| oddest | — | 1 | — | A2 | That is the oddest cat that I've ever met! | infrecuente o sin ranking — se deja penalizada (señal) |
| tidiest | — | 1 | 11187 | A2 | Who has the tidiest desk? | infrecuente o sin ranking — se deja penalizada (señal) |
| endlessly | — | 1 | 9596 | A2 | I became really annoyed as the customer complained endlessly | infrecuente o sin ranking — se deja penalizada (señal) |
| awfully | — | 1 | 8191 | A2 | You cook awfully! | infrecuente o sin ranking — se deja penalizada (señal) |
| nag | — | 1 | 14451 | B1 | She's always nagging her children about their homework. | infrecuente o sin ranking — se deja penalizada (señal) |
| flatmate | — | 1 | — | B1 | I wish that my flatmates wouldn't be so untidy. | infrecuente o sin ranking — se deja penalizada (señal) |
| dye | — | 1 | 16404 | B1 | Has she dyed her hair? | infrecuente o sin ranking — se deja penalizada (señal) |
| meteor | — | 1 | 8731 | B1 | Have you ever witnessed a meteor shower? | infrecuente o sin ranking — se deja penalizada (señal) |
| triathlon | — | 1 | 18273 | B1 | She's completed a triathlon. | infrecuente o sin ranking — se deja penalizada (señal) |
| partie | — | 1 | 10624 | B1 | They partied last night. | infrecuente o sin ranking — se deja penalizada (señal) |
| blaze | — | 1 | 9179 | B1 | What blazed fiercely? | infrecuente o sin ranking — se deja penalizada (señal) |
| fiercely | — | 1 | 9456 | B1 | What blazed fiercely? | infrecuente o sin ranking — se deja penalizada (señal) |
| creamy | — | 1 | 8799 | B1 | What was creamy? | infrecuente o sin ranking — se deja penalizada (señal) |
| dietitian | — | 1 | 19436 | B1 | She went to a dietitian to reduce weight. | infrecuente o sin ranking — se deja penalizada (señal) |
| trendy | — | 1 | 10844 | B1 | Hiking is getting more trendy. | infrecuente o sin ranking — se deja penalizada (señal) |
| yelp | — | 1 | 16888 | B1 | The cat made him yelp. | infrecuente o sin ranking — se deja penalizada (señal) |
| hyper | — | 1 | — | B1 | The cola made me hyper. | infrecuente o sin ranking — se deja penalizada (señal) |
| florist | — | 1 | 16995 | B1 | She bought the flowers from the new florist shop. | infrecuente o sin ranking — se deja penalizada (señal) |
| conditional | — | 1 | 12837 | B1 | Let's analyze the language of conditional sentences. | infrecuente o sin ranking — se deja penalizada (señal) |
| plier | — | 1 | — | B1 | Where is the pliers? not correct | infrecuente o sin ranking — se deja penalizada (señal) |
| legging | — | 1 | 19271 | B1 | Your leggings is in the washing machine. not correct | infrecuente o sin ranking — se deja penalizada (señal) |
| eyeglass | — | 1 | 17619 | B1 | I need an eyeglasses. not correct | infrecuente o sin ranking — se deja penalizada (señal) |
| unclean | — | 1 | 17954 | B1 | May I take the other spoon? This one is unclean. | infrecuente o sin ranking — se deja penalizada (señal) |
| dwell | — | 1 | 9244 | B1 | You need to avoid dwelling on the history. | infrecuente o sin ranking — se deja penalizada (señal) |
| smoothie | — | 1 | — | B1 | She drank every smoothie. | infrecuente o sin ranking — se deja penalizada (señal) |
| seaside | — | 1 | 13643 | B1 | Both David and Emily enjoy the seaside. | infrecuente o sin ranking — se deja penalizada (señal) |
| scold | — | 1 | 11475 | B1 | Leo is being scolded by his mother. | infrecuente o sin ranking — se deja penalizada (señal) |
| prune | — | 1 | 16959 | B1 | The trees will be pruned. | infrecuente o sin ranking — se deja penalizada (señal) |
| modal | — | 1 | — | B1 | Robert said that John had learned modal verbs. | infrecuente o sin ranking — se deja penalizada (señal) |
| vacuum | — | 1 | 14992 | B1 | Robert said that he had vacuumed the living room. | infrecuente o sin ranking — se deja penalizada (señal) |
| sightsee | — | 1 | 18701 | B1 | Robert said that he had been sightseeing. | infrecuente o sin ranking — se deja penalizada (señal) |
| meow | — | 1 | — | B1 | Robert said that the cat had been meowing. | infrecuente o sin ranking — se deja penalizada (señal) |
| fizzy | — | 1 | — | B1 | I mustn't drink so many fizzy drinks! I really want to be he | infrecuente o sin ranking — se deja penalizada (señal) |
| allergic | — | 1 | 8217 | B1 | She mustn't eat coconuts. She's allergic to them. | infrecuente o sin ranking — se deja penalizada (señal) |
| argentinian | — | 1 | — | B1 | You must try the new Argentinian restaurant. | infrecuente o sin ranking — se deja penalizada (señal) |
| meditate | — | 1 | 11194 | B1 | I am supposed to meditate every morning, but I sometimes sle | infrecuente o sin ranking — se deja penalizada (señal) |
| stapler | — | 1 | — | B1 | I borrowed the stapler that you used. object | infrecuente o sin ranking — se deja penalizada (señal) |
| headmaster | — | 1 | 16449 | B1 | We phoned the headmaster who runs the school. | infrecuente o sin ranking — se deja penalizada (señal) |
| repairman | — | 1 | — | B1 | They called the repairman that they trusted. | infrecuente o sin ranking — se deja penalizada (señal) |
| doughnut | — | 1 | 9453 | B1 | That doughnut looked tasty. Could I get another one? | infrecuente o sin ranking — se deja penalizada (señal) |
| sunflower | — | 1 | 14142 | B1 | She cultivated sunflowers and presented her with these lovel | infrecuente o sin ranking — se deja penalizada (señal) |
| devour | — | 1 | 8902 | B1 | Peter devoured a hamburger and I devoured one as well. | infrecuente o sin ranking — se deja penalizada (señal) |
| desolate | — | 1 | 13704 | B1 | He saw everybody / nobody during his journey. It was very de | infrecuente o sin ranking — se deja penalizada (señal) |
| disorganized | — | 1 | — | B1 | She loses her documents everywhere / nowhere! She's so disor | infrecuente o sin ranking — se deja penalizada (señal) |
| recess | — | 1 | 8060 | B1 | The students are excited if it rains during recess. | infrecuente o sin ranking — se deja penalizada (señal) |
| sunscreen | — | 1 | 11634 | B1 | If we go to the beach, we must take sunscreen. | infrecuente o sin ranking — se deja penalizada (señal) |
| brunch | — | 1 | 13164 | B1 | Zero: If it is sunny, we have orange juice for brunch. | infrecuente o sin ranking — se deja penalizada (señal) |
| perplex | — | 1 | 18687 | B1 | Do you believe mathematics is perplexing? | infrecuente o sin ranking — se deja penalizada (señal) |
| rewarding | — | 1 | 9447 | B1 | Gardening is quite rewarding. I love watching plants grow! | infrecuente o sin ranking — se deja penalizada (señal) |
| puzzled | — | 1 | 9128 | B1 | I feel a bit puzzled. I don't quite grasp it. | infrecuente o sin ranking — se deja penalizada (señal) |
| aggravating | — | 1 | 11157 | B1 | What an aggravating situation! | infrecuente o sin ranking — se deja penalizada (señal) |
| ceramic | — | 1 | 9916 | B1 | He drank from a small yellow ceramic cup. | infrecuente o sin ranking — se deja penalizada (señal) |
| smelly | — | 1 | 14455 | B1 | It had smelly round pillows. | infrecuente o sin ranking — se deja penalizada (señal) |
| porcelain | — | 1 | 10460 | B1 | He ate the dessert on a white porcelain dish. | infrecuente o sin ranking — se deja penalizada (señal) |
| calming | — | 1 | 16482 | B1 | A bath is more calming than standing under the shower. | infrecuente o sin ranking — se deja penalizada (señal) |
| tiptoe | — | 1 | 16052 | B1 | We tiptoed past the weak woman in the hallway. | infrecuente o sin ranking — se deja penalizada (señal) |
| doctorate | — | 1 | 10306 | B1 | All the teachers are well-qualified. They all hold doctorate | infrecuente o sin ranking — se deja penalizada (señal) |
| unluckily | — | 1 | — | B1 | Unluckily, he broke his leg. | infrecuente o sin ranking — se deja penalizada (señal) |
| proficient | — | 1 | 12118 | B1 | He practiced daily. Therefore, he became proficient at the p | infrecuente o sin ranking — se deja penalizada (señal) |
| invoice | — | 1 | 15231 | B1 | Did you receive the invoice for the service? | infrecuente o sin ranking — se deja penalizada (señal) |
| novice | — | 1 | 8885 | B1 | I'm a novice at cooking. | infrecuente o sin ranking — se deja penalizada (señal) |
| intern | — | 1 | 15631 | B1 | He interned as a developer. | infrecuente o sin ranking — se deja penalizada (señal) |
| sunbathing | — | 1 | — | B1 | I relax by swimming and sunbathing. | infrecuente o sin ranking — se deja penalizada (señal) |
| dynamite | — | 1 | 12400 | B1 | They demolished the building with dynamite. | infrecuente o sin ranking — se deja penalizada (señal) |
| wrench | — | 1 | 12460 | B1 | We turned the screw with a wrench. | infrecuente o sin ranking — se deja penalizada (señal) |
| stuffy | — | 1 | 16560 | B1 | It felt quite stuffy in the office even with the fan on. | infrecuente o sin ranking — se deja penalizada (señal) |
| wilt | — | 1 | 11160 | B1 | They picked the flowers in time, before they wilted. | infrecuente o sin ranking — se deja penalizada (señal) |
| frodo | — | 1 | — | B2 | Frodo takes the ring and has a dangerous quest. | infrecuente o sin ranking — se deja penalizada (señal) |
| memorize | — | 1 | 8506 | B2 | I won't have memorized the phrases before the exam. | infrecuente o sin ranking — se deja penalizada (señal) |
| purr | — | 1 | 17839 | B2 | How long will the cats have been purring? | infrecuente o sin ranking — se deja penalizada (señal) |
| debug | — | 1 | — | B2 | How long will she have been debugging software? | infrecuente o sin ranking — se deja penalizada (señal) |
| nighttime | — | 1 | 10794 | B2 | Tom's been sleeping since nighttime. | infrecuente o sin ranking — se deja penalizada (señal) |
| paella | — | 1 | — | B2 | I've cooked paella . | infrecuente o sin ranking — se deja penalizada (señal) |
| auntie | — | 1 | 12647 | B2 | She's been visiting her auntie. That's why she looks tired! | infrecuente o sin ranking — se deja penalizada (señal) |
| skyscraper | — | 1 | 11982 | B2 | The workers have built a skyscraper . | infrecuente o sin ranking — se deja penalizada (señal) |
| grease | — | 1 | 8997 | B2 | She's been repairing the bike. She's covered in grease and t | infrecuente o sin ranking — se deja penalizada (señal) |
| finalize | — | 1 | 11486 | B2 | We were eating lunch when we finalized the deal. | infrecuente o sin ranking — se deja penalizada (señal) |
| baked | — | 1 | 8599 | B2 | She'd baked a cake by the time they reached the house. | infrecuente o sin ranking — se deja penalizada (señal) |
| treadmill | — | 1 | 10608 | B2 | She'd been running on the treadmill for a while when she beg | infrecuente o sin ranking — se deja penalizada (señal) |
| invigilate | — | 1 | — | B2 | Won't the teachers be invigilating the test? | infrecuente o sin ranking — se deja penalizada (señal) |
| mary | — | 1 | — | B2 | Doesn't Mary like rice? | infrecuente o sin ranking — se deja penalizada (señal) |
| vegan | — | 1 | 8270 | B2 | Being a vegan, he didn't order the steak. | infrecuente o sin ranking — se deja penalizada (señal) |
| headphone | — | 1 | 9079 | B2 | Are these your headphones? correct | infrecuente o sin ranking — se deja penalizada (señal) |
| pajama | — | 1 | 9480 | B2 | Would you like this pajamas? not correct | infrecuente o sin ranking — se deja penalizada (señal) |
| binocular | — | 1 | 9508 | B2 | Whose are these binoculars? correct | infrecuente o sin ranking — se deja penalizada (señal) |
| tweezer | — | 1 | — | B2 | The tweezers are in the first-aid kit. correct | infrecuente o sin ranking — se deja penalizada (señal) |
| pince | — | 1 | — | B2 | Do you have a pince-nez for reading? not correct | infrecuente o sin ranking — se deja penalizada (señal) |
| tong | — | 1 | 17418 | B2 | Give me that tongs, please. not correct | infrecuente o sin ranking — se deja penalizada (señal) |
| ural | — | 1 | — | B2 | We explored the Urals. | infrecuente o sin ranking — se deja penalizada (señal) |
| joyful | — | 1 | 11497 | B2 | He sang such joyful songs. | infrecuente o sin ranking — se deja penalizada (señal) |
| boutique | — | 1 | 9250 | B2 | The silk scarves that they sell in that boutique are elegant | infrecuente o sin ranking — se deja penalizada (señal) |
| fireman | — | 1 | 10191 | B2 | The cat had been saved by the fireman. | infrecuente o sin ranking — se deja penalizada (señal) |
| pantry | — | 1 | 10330 | B2 | Sarah chose the apples that were stored in the pantry. | infrecuente o sin ranking — se deja penalizada (señal) |
| tailor | — | 1 | 8318 | B2 | I wore a jacket that was designed by a tailor. | infrecuente o sin ranking — se deja penalizada (señal) |
| stray | — | 1 | 9786 | B2 | He feeds stray cats that were abandoned near the store. | infrecuente o sin ranking — se deja penalizada (señal) |
| proofread | — | 1 | — | B2 | She had her essay proofread. | infrecuente o sin ranking — se deja penalizada (señal) |
| remodeled | — | 1 | 12749 | B2 | I am planning to have the kitchen remodeled. | infrecuente o sin ranking — se deja penalizada (señal) |
| wax | — | 1 | 15705 | B2 | They got the floor waxed. | infrecuente o sin ranking — se deja penalizada (señal) |
| plumbing | — | 1 | 9495 | B2 | Robert will get the plumbing checked. | infrecuente o sin ranking — se deja penalizada (señal) |
| wallpaper | — | 1 | 10748 | B2 | He is going to get the hall wallpapered. | infrecuente o sin ranking — se deja penalizada (señal) |
| resole | — | 1 | — | B2 | Does he get his boots resoled? | infrecuente o sin ranking — se deja penalizada (señal) |
| boiler | — | 1 | 12746 | B2 | She's going to get the boiler serviced. | infrecuente o sin ranking — se deja penalizada (señal) |
| overslept | — | 1 | — | B2 | The boy whispered that he had overslept. | infrecuente o sin ranking — se deja penalizada (señal) |
| healthily | — | 1 | — | B2 | You ought to eat more healthily. | infrecuente o sin ranking — se deja penalizada (señal) |
| malfunction | — | 1 | 13533 | B2 | The TV would malfunction all the time. | infrecuente o sin ranking — se deja penalizada (señal) |
| giggly | — | 1 | — | B2 | The girls will often be giggly. | infrecuente o sin ranking — se deja penalizada (señal) |
| limousine | — | 1 | 12204 | B2 | She'll always order a limousine. | infrecuente o sin ranking — se deja penalizada (señal) |
| conditioner | — | 1 | 10333 | B2 | The air conditioner should be on at the moment. | infrecuente o sin ranking — se deja penalizada (señal) |
| dryer | — | 1 | 9956 | B2 | My t-shirt is huge! I shouldn't have dried it in the dryer. | infrecuente o sin ranking — se deja penalizada (señal) |
| sparkly | — | 1 | 19578 | B2 | The jewelry that you wear is very sparkly. that you wear | infrecuente o sin ranking — se deja penalizada (señal) |
| gallop | — | 1 | 15103 | B2 | She stroked the horse that was galloping. Subject | infrecuente o sin ranking — se deja penalizada (señal) |
| pandemic | — | 1 | 12352 | B2 | 2020 was the year. The pandemic started that year. 2020 was  | infrecuente o sin ranking — se deja penalizada (señal) |
| crib | — | 1 | 9843 | B2 | The babies pointed at themselves in the crib. | infrecuente o sin ranking — se deja penalizada (señal) |
| karate | — | 1 | 14185 | B2 | My brother showed himself karate. | infrecuente o sin ranking — se deja penalizada (señal) |
| banquet | — | 1 | 10469 | B2 | If I were the Queen, I'd host a banquet. | infrecuente o sin ranking — se deja penalizada (señal) |
| autograph | — | 1 | 9822 | B2 | If I were famous, I'd sign autographs. | infrecuente o sin ranking — se deja penalizada (señal) |
| eatable | — | 1 | — | B2 | If I hadn't forgotten to turn off the stove, the food would  | infrecuente o sin ranking — se deja penalizada (señal) |
| dehydrate | — | 1 | 18019 | B2 | If you had drunk water, you wouldn't be dehydrated. | infrecuente o sin ranking — se deja penalizada (señal) |
| tempting | — | 1 | 8849 | B2 | The dessert is getting more and more tempting. | infrecuente o sin ranking — se deja penalizada (señal) |
| stimulating | — | 1 | 11489 | B2 | Coffee is more stimulating than drinking tea. | infrecuente o sin ranking — se deja penalizada (señal) |
| decor | — | 1 | 10842 | B2 | She considered the decor a bit new fashioned. | infrecuente o sin ranking — se deja penalizada (señal) |
| grandad | — | 1 | — | B2 | My grandad is a bit of an new-fashioned person. He likes mod | infrecuente o sin ranking — se deja penalizada (señal) |
| botch | — | 1 | 19228 | B2 | She's terrified that she'll botch the test. | infrecuente o sin ranking — se deja penalizada (señal) |
| housemate | — | 1 | — | B2 | Sarah is our prospective housemate. | infrecuente o sin ranking — se deja penalizada (señal) |
| ex | — | 1 | — | B2 | She is his ex classmate. | infrecuente o sin ranking — se deja penalizada (señal) |
| veritable | — | 1 | 15266 | B2 | She is a veritable angel. | infrecuente o sin ranking — se deja penalizada (señal) |
| popcorn | — | 1 | 8268 | B2 | He loves salty snacks, particularly popcorn. | infrecuente o sin ranking — se deja penalizada (señal) |
| cashier | — | 1 | 11576 | B2 | Susan still works as a cashier. | infrecuente o sin ranking — se deja penalizada (señal) |
| overgrown | — | 1 | 18187 | B2 | Though we tidied our garden, it still looked overgrown. | infrecuente o sin ranking — se deja penalizada (señal) |
| boisterously | — | 1 | — | B2 | They laughed the most boisterously. | infrecuente o sin ranking — se deja penalizada (señal) |
| bitterly | — | 1 | 10827 | B2 | She complained the most bitterly. | infrecuente o sin ranking — se deja penalizada (señal) |
| tenderly | — | 1 | 18091 | B2 | Handle this glass the most tenderly. | infrecuente o sin ranking — se deja penalizada (señal) |
| intently | — | 1 | 11472 | B2 | She watched the most intently. | infrecuente o sin ranking — se deja penalizada (señal) |
| cautiously | — | 1 | 9507 | B2 | Who travels the most cautiously? | infrecuente o sin ranking — se deja penalizada (señal) |
| cleverly | — | 1 | 15155 | B2 | She approached the problem the most cleverly. | infrecuente o sin ranking — se deja penalizada (señal) |
| lavender | — | 1 | 9629 | B2 | The bathroom smelled like lavender. | infrecuente o sin ranking — se deja penalizada (señal) |
| lifeguard | — | 1 | 15293 | B2 | They'll work as lifeguards during the vacation. | infrecuente o sin ranking — se deja penalizada (señal) |
| watercolor | — | 1 | 8742 | B2 | I painted the picture with watercolor paints. | infrecuente o sin ranking — se deja penalizada (señal) |
| toolbox | — | 1 | 16993 | B2 | I can fix that with my toolbox. | infrecuente o sin ranking — se deja penalizada (señal) |
| rake | — | 1 | 10017 | B2 | He swept the leaves with a rake. | infrecuente o sin ranking — se deja penalizada (señal) |
| rotary | — | 1 | 14226 | B2 | She cut the fabric with a rotary cutter. | infrecuente o sin ranking — se deja penalizada (señal) |
| hairbrush | — | 1 | — | B2 | She styled her hair with a hairbrush. | infrecuente o sin ranking — se deja penalizada (señal) |
| flip | — | 1 | 11335 | B2 | I turned on the light by flipping the switch. | infrecuente o sin ranking — se deja penalizada (señal) |
| convene | — | 1 | 8676 | B2 | Let's convene at the museum. | infrecuente o sin ranking — se deja penalizada (señal) |
| acquainted | — | 1 | 10254 | B2 | She felt very acquainted with her friend's parents. | infrecuente o sin ranking — se deja penalizada (señal) |
| cognizant | — | 1 | — | B2 | Are you cognizant of the issue? | infrecuente o sin ranking — se deja penalizada (señal) |
| eternally | — | 1 | 14969 | B2 | She's eternally thankful for your generosity. | infrecuente o sin ranking — se deja penalizada (señal) |
| acquaint | — | 1 | 10254 | B2 | Are you acquainted with Rome? | infrecuente o sin ranking — se deja penalizada (señal) |
| allude | — | 1 | 8923 | B2 | The report alluded to the CEO of the corporation. | infrecuente o sin ranking — se deja penalizada (señal) |
| apprehend | — | 1 | 11540 | B2 | The guards apprehended him for robbing the bank. | infrecuente o sin ranking — se deja penalizada (señal) |
| superhero | — | 1 | 8253 | B2 | The young boy put his trust in superheroes. | infrecuente o sin ranking — se deja penalizada (señal) |
| dart | — | 1 | 10301 | B2 | He aimed the dart at the bullseye. | infrecuente o sin ranking — se deja penalizada (señal) |
| bullseye | — | 1 | — | B2 | He aimed the dart at the bullseye. | infrecuente o sin ranking — se deja penalizada (señal) |
| longing | — | 1 | 9705 | B2 | She has no longing for another child. | infrecuente o sin ranking — se deja penalizada (señal) |
| knack | — | 1 | 12438 | B2 | She has a knack for foreign languages. | infrecuente o sin ranking — se deja penalizada (señal) |
| cockroach | — | 1 | 12888 | B2 | They have a hatred of cockroaches. | infrecuente o sin ranking — se deja penalizada (señal) |