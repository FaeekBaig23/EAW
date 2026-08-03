package com.faiqbaig.eaw.core

object CommanderNames {
    val french = listOf("Dubois", "Leclerc", "Ney", "Davout", "Soult", "Lannes", "Murat", "Masséna", "Suchet", "Victor", "Oudinot", "MacDonald", "Marmont", "Moncey", "Mortier", "Jourdan", "Bernadotte", "Augereau", "Brune", "Bessières", "Kellermann", "Lefebvre", "Pérignon", "Sérurier", "Desaix", "Bélanger", "Olise", "Deschamps", "Rabiot", "Rampon", "Fabron")
    val british = listOf("Hill", "Simons", "Clinton", "Wellesley", "Moore", "Picton", "Beresford", "Crawford", "Paget", "Umbridge", "Somerset", "Ponsonby", "Cornwallis", "Pack", "Gage", "Colville", "Cole", "Howe", "Hope", "Graham", "Cotton", "Stewart", "Cunningham", "Rogers", "O'Hara", "Smith", "Swan", "Beckett", "Tarleton", "Montgomery")
    val russian = listOf("Kutuzov", "Barclay", "Bagration", "Bennigsen", "Wittgenstein", "Tormasov", "Chichagov", "Miloradovich", "Dokhturov", "Raevsky", "Ostermann", "Tolstoy", "Ermolov", "Platov", "Uvarov", "Gorchakov", "Tuchkov", "Borozdin", "Neverovsky", "Nevsky", "Vorontsov", "Paskevich", "Diebitsch", "Delny", "Ushakov", "Tartakovsky", "Chkalov")
    val austrian = listOf("Charles", "Schwarzenberg", "Radetzky", "Bellegarde", "Hiller", "Johann", "Ferdinand", "Mack", "Alvinczy", "Melas", "Wurmser", "Beaulieu", "Clerfayt", "Kollowrat", "Liechtenstein", "Rosenberg", "Hohenzollern", "Kienmayer", "Nugent", "Bianchi", "Bubna", "Schindler", "Grau", "Davidovich", "Quadsdanovich", "Gruber")
    val prussian = listOf("Blücher", "Scharnhorst", "Gneisenau", "Bülow", "Zieten", "Yorck", "Kleist", "Tauentzien", "Thielemann", "Wartensleben", "Hohenlohe", "Brunswick", "Rüchel", "L'Estocq", "Kalkreuth", "Courbière", "Götzen", "Tettenborn", "Lützow", "Clausewitz", "Dohna", "Muller", "Schneider", "Hoffmann", "Wagner", "Schmidt", "Albrecht", "Schliefen")

    fun getRandomName(faction: Faction, existingNames: Set<String> = emptySet()): String {
        val pool = when (faction) {
            Faction.FRANCE -> french
            Faction.GREAT_BRITAIN -> british
            Faction.RUSSIA -> russian
            Faction.AUSTRIA -> austrian
            Faction.PRUSSIA -> prussian
        }

        // Filter out names that are already on the field
        val availableNames = pool.filterNot { it in existingNames }

        return if (availableNames.isNotEmpty()) {
            availableNames.random()
        } else {
            pool.random() // Safety fallback if the entire historical roster is deployed
        }
    }
}