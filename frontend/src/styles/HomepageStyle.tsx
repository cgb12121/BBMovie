import styled from 'styled-components';
import { Link } from 'react-router-dom';

export const HomeContainer = styled.div`
  min-height: 100vh;
  background-color: #1a1a1a;
  color: #ffffff;
`;

export const HeroSection = styled.div`
  height: 80vh;
  background: linear-gradient(rgba(0, 0, 0, 0.7), rgba(0, 0, 0, 0.7)),
              url('/images/hero-bg.jpg') center/cover;
  display: flex;
  align-items: center;
  justify-content: center;
  text-align: center;
  padding: 0 20px;
`;

export const HeroContent = styled.div`
  max-width: 800px;
`;

export const HeroTitle = styled.h1`
  font-size: 4rem;
  margin-bottom: 1rem;
  color: #ffffff;
  text-shadow: 2px 2px 4px rgba(0, 0, 0, 0.5);
`;

export const HeroSubtitle = styled.p`
  font-size: 1.5rem;
  margin-bottom: 2rem;
  color: #cccccc;
`;

export const Button = styled(Link)`
  display: inline-block;
  padding: 1rem 2rem;
  background-color: #e50914;
  color: #ffffff;
  text-decoration: none;
  border-radius: 4px;
  font-weight: bold;
  transition: background-color 0.3s;

  &:hover {
    background-color: #f40612;
  }
`;

export const FeaturedSection = styled.section`
  padding: 4rem 2rem;
`;

export const SectionTitle = styled.h2`
  font-size: 2rem;
  margin-bottom: 2rem;
  color: #ffffff;
`;

export const MovieGrid = styled.div`
  display: grid;
  grid-template-columns: repeat(auto-fill, minmax(200px, 1fr));
  gap: 2rem;
  padding: 0 2rem;
`;

export const MovieCard = styled.div`
  background-color: #2a2a2a;
  border-radius: 8px;
  overflow: hidden;
  transition: transform 0.3s;

  &:hover {
    transform: translateY(-5px);
  }
`;

export const MovieImage = styled.img`
  width: 100%;
  height: 300px;
  object-fit: cover;
`;

export const MovieInfo = styled.div`
  padding: 1rem;
`;

export const MovieTitle = styled.h3`
  font-size: 1.1rem;
  margin-bottom: 0.5rem;
  color: #ffffff;
`;

export const MovieRating = styled.div`
  color: #ffd700;
  font-weight: bold;
`;

export const CategoriesSection = styled.section`
  padding: 4rem 2rem;
  background-color: #2a2a2a;
`;

export const CategoryGrid = styled.div`
  display: grid;
  grid-template-columns: repeat(auto-fill, minmax(150px, 1fr));
  gap: 1rem;
  padding: 0 2rem;
`;

export const CategoryCard = styled(Link)`
  background-color: #3a3a3a;
  padding: 1rem;
  border-radius: 4px;
  text-align: center;
  text-decoration: none;
  color: #ffffff;
  transition: background-color 0.3s;

  &:hover {
    background-color: #4a4a4a;
  }
`;