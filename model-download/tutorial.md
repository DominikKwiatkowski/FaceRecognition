## Script configuration tutorial
To run ```main.py``` script included in given directory:

* install Python 3.8.x
* install package manager - pip
* make sure that both are included in environmental variables
* install package allowing creation of virtual environment: ```pip install virtualenv```
![](https://i.imgur.com/gZjzftQ.png)
* create virtual environment in current location and activate it with
  * ```virtualenv {dir}```
![](https://i.imgur.com/k0KZkAS.png)
  * ```source Scripts/activate```
![](https://i.imgur.com/iHv7Yar.png)
* install all packages from requirements.txt: ```pip install -r requirements.txt```
![](https://i.imgur.com/4XP7e0b.png)
* run the script: ```python main.py```
![](https://i.imgur.com/SU3xYJq.png)

First execution and download of the model might take a while and also some space on the disk - location of downloaded files is displayed in console during the process and can also be found in DeepFace documentation