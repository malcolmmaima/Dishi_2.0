# Dishi_2.0
This is a realtime food ordering app running on a realtime firebase backend. Generate your own package.json from firebase console and have a go at it. live apk is also available for testing in the link below.

https://malcolmmaima.com/dishi

## System Overview

Dishi is a front-end client which talks to the Firebase Database. It implements Google Maps API for live tracking latitude/longitude coordinates of user’s device once logged in. The live tracking module is given high priority and given most attention in terms of avoiding bugs and interface deficiencies. User Authentication is done using Google’s phone number authentication where a special code is sent to the phone number entered before accessing the system, this bars spam accounts as each account is linked to a phone number.

<a href="url"><img src=https://github.com/malcolmmaima/Dishi_2.0/assets/3639153/4a68dace-ca1b-459c-b927-fca41426f8a4 height="450"></a>
<a href="url"><img src=https://github.com/malcolmmaima/Dishi_2.0/assets/3639153/b5eec7ef-abd6-41e1-8b68-9eda818470b8 height="450"></a>
<a href="url"><img src=https://github.com/malcolmmaima/Dishi_2.0/assets/3639153/52461156-961a-4f0f-86cd-adc3b8bbc6b0 height="450"></a>
<a href="url"><img src=https://github.com/malcolmmaima/Dishi_2.0/assets/3639153/b24e18fd-e7d4-41d6-83ba-ed02422583dd height="450"></a>
<a href="url"><img src=https://github.com/malcolmmaima/Dishi_2.0/assets/3639153/43126c4a-add5-469b-9949-f27f5965921c height="450"></a>
<a href="url"><img src=https://github.com/malcolmmaima/Dishi_2.0/assets/3639153/5521e630-62bb-462c-b7d4-c28039d2dbfc height="450"></a>
<a href="url"><img src=https://github.com/malcolmmaima/Dishi_2.0/assets/3639153/fe414b38-0e74-4b87-8cae-b78722ec2be4 height="450"></a>
<a href="url"><img src=https://github.com/malcolmmaima/Dishi_2.0/assets/3639153/e8c70062-1169-4393-8194-d518793f62c9 height="450"></a>
<a href="url"><img src=https://github.com/malcolmmaima/Dishi_2.0/assets/3639153/3d6fca1c-6269-494e-8910-bb493db764fe height="450"></a>
<a href="url"><img src=https://github.com/malcolmmaima/Dishi_2.0/assets/3639153/757f0180-49fa-420b-bebb-cb8c965076da height="450"></a>
<a href="url"><img src=https://github.com/malcolmmaima/Dishi_2.0/assets/3639153/42b712b4-b0b2-4340-bcfc-807143759cbc height="450"></a>
<a href="url"><img src=https://github.com/malcolmmaima/Dishi_2.0/assets/3639153/4ab00820-66ce-4b0f-a73e-65221a11a58b height="450"></a>
<a href="url"><img src=https://github.com/malcolmmaima/Dishi_2.0/assets/3639153/658a91ed-31ff-44f4-8ad7-ed2bfd4bba9c height="450"></a>
<a href="url"><img src=https://github.com/malcolmmaima/Dishi_2.0/assets/3639153/1f41f7f3-7911-4549-b974-42bcdc2a8804 height="450"></a>
<a href="url"><img src=https://github.com/malcolmmaima/Dishi_2.0/assets/3639153/2ba8546c-8209-4fdd-b799-1c7fdd228506 height="450"></a>
<a href="url"><img src=https://github.com/malcolmmaima/Dishi_2.0/assets/3639153/7caf3b6f-7983-4091-a9e5-b5e51a2cef84 height="450"></a>
<a href="url"><img src=https://github.com/malcolmmaima/Dishi_2.0/assets/3639153/94a64505-a6d9-4c93-81be-6f5fc5220692 height="450"></a>
<a href="url"><img src=https://github.com/malcolmmaima/Dishi_2.0/assets/3639153/2bff02fd-224d-4220-b44f-1cde426bbcf2 height="450"></a>
<a href="url"><img src=https://github.com/malcolmmaima/Dishi_2.0/assets/3639153/03e0b1ac-8fbb-4107-824b-03d4d2283725 height="450"></a>
<a href="url"><img src=https://github.com/malcolmmaima/Dishi_2.0/assets/3639153/d291d81c-84c4-4ecc-afdd-908befea0841 height="450"></a>
<a href="url"><img src=https://github.com/malcolmmaima/Dishi_2.0/assets/3639153/6acfc3ca-b2c8-4ef1-8ca6-6ecb2c9a6257 height="450"></a>
<a href="url"><img src=https://github.com/malcolmmaima/Dishi_2.0/assets/3639153/45056482-e59d-4365-8a36-ff7cc965382e height="450"></a>
<a href="url"><img src=https://github.com/malcolmmaima/Dishi_2.0/assets/3639153/564634c1-ebc0-44db-ac3d-5d855138a346 height="450"></a>
<a href="url"><img src=https://github.com/malcolmmaima/Dishi_2.0/assets/3639153/f4f2f7fb-2986-4170-b28c-8cf37ac3d066 height="450"></a>
<a href="url"><img src=https://github.com/malcolmmaima/Dishi_2.0/assets/3639153/35ca0f6f-2dff-4b98-989f-51adbcdeffea height="450"></a>
<a href="url"><img src=https://github.com/malcolmmaima/Dishi_2.0/assets/3639153/f28495c6-a001-49be-80e5-5fd0d6d30e0a height="450"></a>
<a href="url"><img src=https://github.com/malcolmmaima/Dishi_2.0/assets/3639153/fddecfb8-d35d-4470-be6a-cd1703e400d5 height="450"></a>
<a href="url"><img src=https://github.com/malcolmmaima/Dishi_2.0/assets/3639153/eeb10461-7aee-4d70-ac8e-6974eb4f8ed9 height="450"></a>


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
