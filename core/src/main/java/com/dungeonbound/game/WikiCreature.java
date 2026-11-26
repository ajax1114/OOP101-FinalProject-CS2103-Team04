package com.dungeonbound.game;

import com.badlogic.gdx.graphics.Color;

import java.util.Arrays;
import java.util.Collections;
import java.util.List;

/** Data ng bawat creature sa wiki. Dagdag lang ng bagong linya sa ALL para sa bagong card. */
public final class WikiCreature {

    /** Laki ng isang frame sa spritesheet (assets/wiki/anim_<id>.png), 9 frames na nakahilera. */
    public static final int FRAME_SIZE = 256;
    public static final int FRAME_COUNT = 9;

    public final String id;        // gamitin ito sa WikiProgress.unlock("tiyanak")
    public final int number;       // 1 -> "001"
    public final String name;

    public final String cardRegion;// region name sa wiki.atlas ("card_<id>")
    public final String animRegion;// region name sa wiki.atlas ("anim_<id>")
    public final String blurRegion;// region name sa wiki.atlas ("blur_<id>")
    public final String cardFile;  // legacy path
    public final String animFile;  // legacy path
    public final String blurFile;  // legacy path
=======
    public final String cardFile;  // sa assets/ (card sa menu)
    public final String animFile;  // sa assets/ (spritesheet ng LoreScreen)
    public final String blurFile;  // sa assets/ (blurred card sa likod ng enemy sa LoreScreen)

    public final String loreTl;    // Tagalog lore
    public final String loreEn;    // English lore
    public final Color bg;         // kulay ng buong background
    public final Color panel;      // kulay ng lore panel
    public final Color bar;        // kulay ng title bar

    private WikiCreature(String id, int number, String name, String loreTl, String loreEn,
                         Color bg, Color panel, Color bar) {
        this.id = id; this.number = number; this.name = name;

        this.cardRegion = "card_" + id;
        this.animRegion = "anim_" + id;
        this.blurRegion = "blur_" + id;

        this.cardFile = "wiki/card_" + id + ".png";
        this.animFile = "wiki/anim_" + id + ".png";
        this.blurFile = "wiki/blur_" + id + ".png";
        this.loreTl = loreTl;
        this.loreEn = loreEn;
        this.bg = bg; this.panel = panel; this.bar = bar;
    }

    public String getLore() {
        return Lang.isTL() ? loreTl : loreEn;
    }

    private static Color rgb(int r, int g, int b) { return new Color(r / 255f, g / 255f, b / 255f, 1f); }

    public static final List<WikiCreature> ALL = Collections.unmodifiableList(Arrays.asList(

        new WikiCreature("tiyanak", 1, "Tiyanak",
            "Ang Tiyanak ay isang nilalang sa mitolohiyang Pilipino na nagpapanggap bilang umiiyak na "
          + "sanggol upang akitin ang mga tao. Karaniwan itong lumilitaw sa gilid ng daan, gubat, bukid, "
          + "o latian; kapag nilapitan o hinawakan, nagpapakita ito ng tunay nitong anyo at inaatake ang biktima."
          + "\n\n"
          + "Madalas inilalarawan ang Tiyanak bilang mapanlinlang at mapanganib, hindi basta-bastang bata. "
          + "Ginagamit nito ang hitsura at iyak ng isang inosenteng sanggol para makuha ang tiwala ng biktima; "
          + "kapag nasa malapit na ito, saka nito ipinapakita ang tunay na anyo."
          + "\n\n"
          + "May mga kuwento ring ang Tiyanak ay kumukuha o dinadala ang mga bata o matatandang tao, at minsan "
          + "ay nagpapakita bilang grupo sa mga bundok o gubat."
          + "\n\n"
          + "Ang Tiyanak ay itinuturing na isa sa mga kilalang nilalang sa Philippine folklore. Sa maraming "
          + "kuwento, ito ay laging mapanganib at gumagamit ng anyo ng sanggol upang makuha ang kumpiyansa ng mga tao.",

            "The Tiyanak is a creature from Philippine mythology that mimics the cries of a baby to lure "
          + "unsuspecting travelers. It commonly appears along roadsides, deep forests, open fields, "
          + "or swamps; once approached or touched, it reveals its true, monstrous form and attacks its victim."
          + "\n\n"
          + "The Tiyanak is often depicted as cunning and lethal, far from a harmless child. "
          + "It uses the guise and wails of an innocent infant to gain the victim's trust; "
          + "only when they draw close does it unveil its sharp claws, fangs, and predatory nature."
          + "\n\n"
          + "Folktales also tell of Tiyanaks abducting children or elderly wanderers, leading them "
          + "astray into desolate wilderness, occasionally appearing in packs across mountains and dense forests."
          + "\n\n"
          + "Regarded as one of the most iconic creatures in Philippine folklore, the Tiyanak remains an enduring "
          + "symbol of deceit in the wild, preying upon human compassion and the instinct to protect the helpless.",
            rgb(49, 18, 25), rgb(66, 23, 23), rgb(129, 27, 27)),

        new WikiCreature("kapre", 2, "Kapre",
            "Ang Kapre ay isang nilalang sa mitolohiyang Pilipino na naninirahan sa malalaking matatandang puno, "
          + "lalo na sa balete, akasya, at mangga. Karaniwan itong lumilitaw sa gubat, bukid, liblib na daan, o "
          + "malapit sa mga puno; kapag nalapitan o nagalit, maaari nitong litawin, ligawin, o saktan ang tao."
          + "\n\n"
          + "Madalas inilalarawan ang Kapre bilang higanteng, itim, at mabalahibong nilalang na nakaupo sa sanga "
          + "habang nanunog ng malaking tabako o sigarilyo. Ginagamit nito ang amoy ng usok, liwanag ng sigarilyo "
          + "sa dilim, tawa, at ingay mula sa itaas upang takutin ang mga dumadaan."
          + "\n\n"
          + "May mga kuwento ring ang Kapre ay nagpapalit ng sukat at nagpapakita bilang ordinaryong tao upang "
          + "makalapit sa mga tao. Sa ilang salaysay, naaakit ito sa magagandang babae at minsan ay nagpapakita "
          + "bilang grupo sa mga gubat o bundok."
          + "\n\n"
          + "Ang Kapre ay hindi laging masamang nilalang. Sa maraming kuwento, ito ay tagapagbantay ng puno at "
          + "nagpaparusa lamang sa mga taong nag-iingay, nanghihiwa ng sanga, o sumusunog ng kagubatan.",

            "The Kapre is a legendary giant from Philippine mythology that dwells in large, ancient trees, "
          + "especially balete, acacia, and mango trees. It lurks within forests, farmlands, lonely paths, "
          + "or near sacred groves; if provoked or encroached upon, it can disorient, lead travelers astray, or inflict harm."
          + "\n\n"
          + "Often depicted as a towering, dark, and hairy giant sitting atop thick branches while smoking an "
          + "endless cigar. It uses the pungent smell of burning tobacco, glowing embers in the canopy, "
          + "deep laughter, and rustling boughs to startle passersby in the dead of night."
          + "\n\n"
          + "Stories tell that Kapres can shift their size to blend among humans or pursue their desires. "
          + "In several regional legends, they are known to become infatuated with beautiful mortal women, "
          + "guarding their homes from the shadows."
          + "\n\n"
          + "A Kapre is not entirely malevolent by nature. In many tales, it acts as a sentinel of ancient trees, "
          + "punishing only those who disrespect nature by making reckless noise, cutting branches, or burning sacred groves.",
            rgb(15, 15, 15), rgb(31, 29, 28), rgb(25, 16, 16)),

        new WikiCreature("aswang", 3, "Aswang",
            "Ang Aswang ay isang nilalang sa mitolohiyang Pilipino na nagpapanggap bilang ordinaryong tao sa "
          + "araw. Karaniwan itong lumilitaw sa mga barangay, gubat, bukid, o malalayong lugar; kapag dumating "
          + "ang gabi, nagpapakita ito ng tunay nitong anyo at inaatake ang mga tao."
          + "\n\n"
          + "Madalas inilalarawan ang Aswang bilang mapanlinlang at mapanganib, hindi basta-bastang tao. "
          + "Ginagamit nito ang hitsura ng ordinaryong tao upang makalapit sa mga tao; kapag nasa malapit na "
          + "ito, saka nito ipinapakita ang tunay nitong anyo."
          + "\n\n"
          + "May mga kuwento ring ang Aswang ay kumakain ng bangkay, sumisipsip ng dugo, o kumakain ng laman "
          + "ng mga tao. Sa ilang lugar, ito ay nagpapakita bilang hayop, ibon, o iba pang anyo."
          + "\n\n"
          + "Ang Aswang ay hindi isang iisang nilalang lamang. Sa maraming kuwento, ito ay pangkalahatang "
          + "tawag para sa iba't ibang mapanganib na nilalang na kumakain ng laman, sumisipsip ng dugo, o "
          + "gumagamit ng masamang salamangka.",

            "The Aswang is a shape-shifting entity from Philippine folklore that masquerades as an ordinary person "
          + "by daylight. Living quietly among villages, rural outskirts, or woodland fringes, it drops its mortal "
          + "facade when darkness falls to stalk human prey."
          + "\n\n"
          + "Renowned for its cunning deception, the Aswang is deadly and elusive. It blends seamlessly with everyday "
          + "townsfolk to observe habits and discover vulnerabilities before striking when its targets are isolated."
          + "\n\n"
          + "Folklore recounts diverse variations: some feed on flesh, others drink blood or steal corpses, "
          + "transforming into black dogs, wild boars, or nocturnal birds to prowl unnoticed beneath the moon."
          + "\n\n"
          + "Rather than a single species, 'Aswang' serves as an umbrella term for a variety of predatory ghouls, "
          + "vampires, and dark sorcerers, representing the deep dread of supernatural terror in Philippine mythology.",
            rgb(18, 34, 49), rgb(33, 50, 67), rgb(22, 37, 60)),

        new WikiCreature("tikbalang", 4, "Tikbalang",
            "Ang Tikbalang ay isang nilalang sa mitolohiyang Pilipino na naninirahan sa mga bundok, gubat, at "
          + "malalayong daan. Karaniwan itong lumilitaw sa mga lugar kung saan madaling maligaw ang mga tao; "
          + "kapag nalapitan o nakita, maaari nitong litawin, ligawin, o patawanin ang tao."
          + "\n\n"
          + "Madalas inilalarawan ang Tikbalang bilang matangkad, mahabang binti, may paa ng kabayo, at ulo ng "
          + "kabayo. Ginagamit nito ang paglilitaw, pag-imbento ng mga tunog, at pagliligaw ng daan upang "
          + "maligaw o matakot ang mga dumadaan."
          + "\n\n"
          + "May mga kuwento ring ang Tikbalang ay nagpapakita bilang buong kabayo, nagpapalit ng anyo, at "
          + "maaaring makuha ang isang espesyal na buhok nito upang masakop o maging kaibigan."
          + "\n\n"
          + "Ang Tikbalang ay hindi laging masamang nilalang. Sa maraming kuwento, ito ay mapanlinlang o "
          + "mapang-asar, ngunit maaari ring maging tagapagbantay ng gubat o bundok kung igagalang ng mga tao.",

            "The Tikbalang is a mythical trickster dwelling in remote mountains, forests, and desolate crossroads. "
          + "It lurks where travelers easily lose their bearings, using illusions and shifting landmarks to trap "
          + "wanderers in endless circles."
          + "\n\n"
          + "Typically described as an extraordinarily tall creature with disproportionately long limbs, hooves, "
          + "and the head of an equine stallion. It disorients travelers through phantom noises, mimicking familiar "
          + "voices and rearranging paths in the woods."
          + "\n\n"
          + "Tradition says one can tame a wild Tikbalang by leaping onto its back during a frenzy and plucking a "
          + "golden spine from its mane, transforming the mischievous beast into a loyal servant."
          + "\n\n"
          + "The Tikbalang is rarely outright evil, but rather an unpredictable trickster and guardian of mountain "
          + "wilderness, turning its wrath only against those who dishonor the sanctity of the wilderness.",
            rgb(20, 49, 18), rgb(35, 67, 33), rgb(25, 48, 19)),

        new WikiCreature("manananggal", 5, "Manananggal",
            "Ang Manananggal ay isang nilalang sa mitolohiyang Pilipino na nagpapanggap bilang ordinaryong tao "
          + "sa araw, karaniwang babae. Karaniwan itong lumilitaw sa mga liblib na lugar, gubat, o malalayong "
          + "bahay; kapag dumating ang gabi, naghihiwalay ang itaas nitong katawan mula sa ibabang bahagi at "
          + "lumilipad ito upang manginain."
          + "\n\n"
          + "Madalas inilalarawan ang Manananggal bilang mapanlinlang at mapanganib, hindi basta-bastang tao. "
          + "Ginagamit nito ang itsura ng ordinaryong babae upang makalapit sa mga tao; kapag gabi na, saka "
          + "nito ipinapakita ang tunay nitong anyo."
          + "\n\n"
          + "May mga kuwento ring ang Manananggal ay sumisipsip ng dugo, inaatake ang mga buntis o sanggol, at "
          + "maaaring patayin kung matatagpuan ang ibabang bahagi ng katawan nito bago ito makabalik."
          + "\n\n"
          + "Ang Manananggal ay itinuturing na isa sa mga kilalang nilalang na kaugnay ng aswang. Sa maraming "
          + "kuwento, ito ay laging mapanganib at kumakain o sumisipsip ng dugo ng mga tao.",

            "The Manananggal is a terrifying creature from Philippine folklore appearing by day as an unassuming woman. "
          + "Under the cover of night, its upper torso severs entirely from its lower body, taking flight with bat-like "
          + "wings in search of victims."
          + "\n\n"
          + "Deceptively harmless during waking hours, it stalks secluded barrios and rural rooftops after sunset, "
          + "perching above thatched roofs while extending an elongated, tubular tongue to siphon the life of its sleeping prey."
          + "\n\n"
          + "Infamous for targeting pregnant mothers and infants, its greatest vulnerability lies in its grounded lower half; "
          + "scattering salt, ash, or crushed garlic over the severed torso prevents reconnection, destroying it at sunrise."
          + "\n\n"
          + "One of the most widely feared monsters in Philippine demonology, the Manananggal stands as a haunting "
          + "testament to nocturnal danger and fractured duality in local myth.",
            rgb(21, 18, 49), rgb(43, 33, 67), rgb(24, 20, 48))
    ));
}
