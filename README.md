# Dishi_2.0
This is a realtime food ordering app running on a realtime firebase backend. Generate your own package.json from firebase console and have a go at it. live apk is also available for testing in the link below.

https://malcolmmaima.com/dishi

## System Overview

Dishi is a front-end client which talks to the Firebase Database. It implements Google Maps API for live tracking latitude/longitude coordinates of user’s device once logged in. The live tracking module is given high priority and given most attention in terms of avoiding bugs and interface deficiencies. User Authentication is done using Google’s phone number authentication where a special code is sent to the phone number entered before accessing the system, this bars spam accounts as each account is linked to a phone number.

<a href="url"><img src=https://github.com/malcolmmaima/Dishi_2.0/assets/3639153/97f06b6d-382a-4445-b820-92993f53fabe height="550"></a>
<a href="url"><img src=https://github.com/malcolmmaima/Dishi_2.0/assets/3639153/a635a93e-c9b6-416a-94c8-b400df1e3aff height="550"></a>
<a href="url"><img src=https://github.com/malcolmmaima/Dishi_2.0/assets/3639153/b72e7f89-f2e0-44d8-9f4e-d5b94c57eb13 height="550"></a>
<a href="url"><img src=https://github.com/malcolmmaima/Dishi_2.0/assets/3639153/9a23564d-d8d0-4fc9-952a-57e93189fce2 height="550"></a>
<a href="url"><img src=https://github.com/malcolmmaima/Dishi_2.0/assets/3639153/2be97a48-8b06-4416-b1fb-e2f38169d946 height="550"></a>
<a href="url"><img src=https://github.com/malcolmmaima/Dishi_2.0/assets/3639153/2b53824d-404a-470c-9544-39abf37b6395 height="550"></a>
<a href="url"><img src=https://github.com/malcolmmaima/Dishi_2.0/assets/3639153/c50666b3-0b9d-44a9-8477-8a329f1b22f8 height="550"></a>
<a href="url"><img src=https://github.com/malcolmmaima/Dishi_2.0/assets/3639153/7ac81a3c-7abb-4d8a-bb1a-ecf1d68aa674 height="550"></a>
<a href="url"><img src=https://github.com/malcolmmaima/Dishi_2.0/assets/3639153/e64250d4-8ceb-4342-b811-7db3fd2fd9c3 height="550"></a>
<a href="url"><img src=https://github.com/malcolmmaima/Dishi_2.0/assets/3639153/28c8ea1b-a8e4-4e92-9f0c-ec3eff7aea46 height="550"></a>
<a href="url"><img src=https://github.com/malcolmmaima/Dishi_2.0/assets/3639153/cbb139ff-8f10-4c71-80fc-d204f217ee1e height="550"></a>
<a href="url"><img src=https://github.com/malcolmmaima/Dishi_2.0/assets/3639153/b6a8d6e6-e3c6-41f0-89dd-f961708a0d9c height="550"></a>
<a href="url"><img src=https://github.com/malcolmmaima/Dishi_2.0/assets/3639153/d9a05aba-3dc5-4421-98e6-c38c5ec39d38 height="550"></a>

### Realtime Tracking => https://www.instagram.com/p/CFZF9VyBcKq/

## How it works

Dishi Food is a real-time food ordering service. As a free service, dishi allows literally anyone who loves to cook, bake etc to have a go at a quick food ordering app. As a customer, vendors and food items are tailored based on your geo-location to give you the most convenient service. As a vendor, you’re able to receive orders in real-time and make those deliveries. Think of it as an affordable Uber Eats of sorts.

## Ordering Process

The ordering process is very straightforward, once customer logs into their account they are able to set location preferences using the slider to the geographical radius they want to search. This gives a personalized experience in that each user only sees menu items within a set geographical radius. After a list of nearby menu items is fetched and displayed, they are able to add to Cart and if satisfied can complete the order. Once the provider confirms their order, they can monitor the status of their order in real-time using the tracking module.

After adding items to Cart, you can view your cart and complete the order, this sends the order to the respective provider(s) who will receive a notification of the order items.

## Confirming Order

The provider receives order notifications in real-time from multiple customers and is able to prioritize on which ones he/she wants to fulfil first. The orders are stacked on a first come basis and the provider is able to confirm or decline on a rolling basis. On confirmation the status is sent to customer as you saw earlier which changes the indicator to green. On the provider end, he/she is able to track individual item customer’s location and deliver to the exact location the customer is in.

## Collaboration
I welcome any kind of collaboration on this project, feel free to shoot me an email @ malcolmmaima [at] gmail [dot] com
