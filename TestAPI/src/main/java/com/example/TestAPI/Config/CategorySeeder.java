package com.example.TestAPI.Config;

import com.example.TestAPI.Model.JobCategory;
import com.example.TestAPI.Repository.JobCategoryRepository;
import jakarta.annotation.PostConstruct;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Component;

import java.util.List;

@Component
@RequiredArgsConstructor
@Slf4j
public class CategorySeeder {

    private final JobCategoryRepository categoryRepository;

    @PostConstruct
    public void seed() {
        if (categoryRepository.count() > 0) {
            log.info("Catégories déjà présentes, skip seed");
            return;
        }

        JobCategory domicile = createParent("Services à domicile", "🧹", 1);
        JobCategory bricolage = createParent("Bricolage & maintenance", "🔧", 2);
        JobCategory jardin = createParent("Jardinage & extérieur", "🌿", 3);
        JobCategory aide = createParent("Aide aux personnes", "👤", 4);
        JobCategory livraison = createParent("Livraison & courses", "📦", 5);
        JobCategory tech = createParent("Technologie & assistance", "💻", 6);

        createChild(domicile, "Ménage", "Nettoyage complet ou partiel du domicile", 1);
        createChild(domicile, "Repassage", "Repassage et pliage du linge", 2);
        createChild(domicile, "Cuisine / aide culinaire", "Préparation de repas, courses cuisine", 3);

        createChild(bricolage, "Réparations simples", "Plomberie, électricité de base", 1);
        createChild(bricolage, "Montage de meubles", "Assemblage de meubles en kit", 2);
        createChild(bricolage, "Peinture / petits travaux", "Peinture, rebouchage, petites rénovations", 3);

        createChild(jardin, "Tonte de pelouse", "Tonte et bordure de jardin", 1);
        createChild(jardin, "Entretien de jardin", "Désherbage, taille, plantation", 2);
        createChild(jardin, "Nettoyage extérieur", "Nettoyage terrasse, façade, voiture", 3);

        createChild(aide, "Babysitting", "Garde d'enfants ponctuelle ou régulière", 1);
        createChild(aide, "Aide aux personnes âgées", "Accompagnement, courses, compagnie", 2);
        createChild(aide, "Soutien scolaire", "Cours particuliers, aide aux devoirs", 3);

        createChild(livraison, "Courses alimentaires", "Faire les courses et livrer à domicile", 1);
        createChild(livraison, "Livraison locale", "Livraison de colis, documents, repas", 2);
        createChild(livraison, "Dépannage urgent", "Récupérer un colis, courses express", 3);

        createChild(tech, "Dépannage informatique", "Réparation ordinateur, installation", 1);
        createChild(tech, "Installation logiciels", "Installation et configuration de logiciels", 2);
        createChild(tech, "Aide smartphone / internet", "Configuration téléphone, wifi, imprimante", 3);

        categoryRepository.flush();
        log.info("Catégories MVP créées avec succès");
    }

    private JobCategory createParent(String name, String icon, int order) {
        JobCategory cat = JobCategory.builder()
                .name(name)
                .icon(icon)
                .displayOrder(order)
                .build();
        return categoryRepository.save(cat);
    }

    private void createChild(JobCategory parent, String name, String description, int order) {
        JobCategory cat = JobCategory.builder()
                .name(name)
                .description(description)
                .parent(parent)
                .displayOrder(order)
                .build();
        categoryRepository.save(cat);
    }
}
